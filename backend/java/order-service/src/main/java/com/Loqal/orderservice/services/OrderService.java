package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.OrderStatus;
import com.Loqal.orderservice.dto.ProductOrderRequest;
import com.Loqal.orderservice.dto.events.OrderCancellationEvent;
import com.Loqal.orderservice.dto.events.StockReservationResponse;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.entity.OrderItem;
import com.Loqal.orderservice.entity.Product;
import com.Loqal.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient productServiceWebClient;
    private final OutboxService outboxService;

    @Value("${spring.kafka.topic.order-cancel}") // Standardized topic name
    private String orderCancellationTopic;


    private void triggerStockReversion(Order order) {
        List<ProductOrderRequest> itemsToRevert = order.getItems().stream()
                .map(item -> new ProductOrderRequest(item.getProductId(), item.getPriceAtPurchase(), item.getQuantity()))
                .collect(Collectors.toList());
        OrderCancellationEvent event = new OrderCancellationEvent(order.getId(), itemsToRevert);
        outboxService.requestStockReversion(event);
    }

    public Mono<Order> createOrder(OrderRequest req, UUID userId) {
        Order order = new Order();
        order.setCustomerId(userId);
        order.setCreatedAt(LocalDateTime.now());
        order.setCurrentStatus(OrderStatus.ORDER_PENDING);

        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    return Flux.fromIterable(req.getItems())
                            .flatMap(itemReq ->
                                    productServiceWebClient
                                            .get()
                                            .uri("http://product-service/api/products/{id}", itemReq.getProductId())
                                            .retrieve()
                                            .bodyToMono(Product.class)
                                            .timeout(Duration.ofSeconds(5))
                                            .retry(2)
                                            .map(product -> Tuples.of(product, itemReq.getQuantity()))
                                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found: " + itemReq.getProductId())))
                            )
                            .collectList()
                            .flatMap(productTuples -> {
                                List<OrderItem> orderItems = productTuples.stream()
                                        .map(tuple -> {
                                            Product product = tuple.getT1();
                                            Integer quantity = tuple.getT2();

                                            OrderItem orderItem = new OrderItem();
                                            orderItem.setProductId(product.getId());
                                            orderItem.setQuantity(quantity);
                                            orderItem.setPriceAtPurchase(product.getPrice());
                                            orderItem.setOrderId(savedOrder.getId());
                                            return orderItem;
                                        })
                                        .collect(Collectors.toList());

                                savedOrder.setItems(orderItems);

                                double totalPrice = orderItems.stream()
                                        .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                                        .sum();
                                savedOrder.setFinalAmount(totalPrice);

                                // 3. Save the updated order with items and price
                                return orderRepository.save(savedOrder)
                                        .doOnSuccess(finalOrder -> {
                                            try {
                                                outboxService.requestStockReservation(finalOrder);
                                            } catch (Exception e) {
                                                log.error("Failed to create outbox event for order {}", finalOrder.getId(), e);
                                            }
                                        });
                            });
                });
    }

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.stock-reservation-result}", groupId = "order-service-group")
    public Mono<Void> consumeStockReservationResult(StockReservationResponse response) {
        log.info("Received stock reservation result for order {}: {}", response.getOrderId(), response.getStatus());

        return orderRepository.findById(response.getOrderId())
                .flatMap(order -> {
                    if (order.getCurrentStatus() == OrderStatus.ORDER_CANCELLED_PENDING) {
                        log.warn("Stock result received for order {} that was already marked for cancellation.", order.getId());

                        if ("SUCCESS".equals(response.getStatus())) {
                            triggerStockReversion(order);
                            log.info("Triggering stock reversion for successfully reserved but cancelled order {}.", order.getId());
                        }
                        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
                    }

                    else if (order.getCurrentStatus() == OrderStatus.ORDER_PENDING) {
                        if ("SUCCESS".equals(response.getStatus())) {
                            order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);
                        } else {
                            order.setCurrentStatus(OrderStatus.ORDER_REJECTED);
                        }
                    }

                    else {
                        log.warn("Received stock result for order {} in an unexpected state: {}. Ignoring message.", order.getId(), order.getCurrentStatus());
                        return Mono.empty();
                    }

                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .doOnError(e -> log.error("Error processing stock reservation result for order {}. The message will be retried.", response.getOrderId(), e))
                .then();
    }

    @Transactional
    public Mono<Void> cancelOrder(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found: " + orderId)))
                .flatMap(order -> {
                    if (!order.getCustomerId().equals(userId)) {
                        return Mono.error(new SecurityException("Unauthorized: You do not own this order."));
                    }

                    if (order.getCurrentStatus() == OrderStatus.ORDER_PENDING) {
                        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED_PENDING);
                        log.info("Order {} marked for cancellation. Waiting for stock reservation result.", orderId);
                    } else if (order.getCurrentStatus() == OrderStatus.ORDER_CONFIRMED) {
                        triggerStockReversion(order);
                        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
                        log.info("Scheduled stock reversion for cancelled order {}", orderId);
                    } else {
                        return Mono.error(new IllegalStateException("Order cannot be cancelled in its current state: " + order.getCurrentStatus()));
                    }
                    return orderRepository.save(order);
                }).then();
    }

    public Flux<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findAllByCustomerId(userId);
    }

    public Mono<Order> getOrderByIdAndUserId(UUID orderId, UUID userId) {
        return orderRepository.findByCustomerIdAndId(userId, orderId)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found or you do not have permission to view it.")));
    }

    public Flux<Order> getOrdersByMerchantId(UUID merchantId) {
        return orderRepository.findAllByMerchantId(merchantId);
    }
}