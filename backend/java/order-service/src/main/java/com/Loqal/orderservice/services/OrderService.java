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
import reactor.core.scheduler.Schedulers;
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
//    private final KafkaTemplate<String, Object> kafkaTemplate;
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

        // 1. Create a reactive stream (Flux) from the list of items in the request.
        return Flux.fromIterable(req.getItems())
                // 2. For each item, make a non-blocking call to the product-service.
                //    flatMap is used to handle the async nature of the call.
                .flatMap(itemReq ->
                        productServiceWebClient
                                .get()
                                .uri("http://product-service/api/products/{id}", itemReq.getProductId())
                                .retrieve()
                                .bodyToMono(Product.class)
                                .timeout(Duration.ofSeconds(5))  // Add this
                                .retry(2)  // Add this for resilience
                                .map(product -> Tuples.of(product, itemReq.getQuantity()))
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found: " + itemReq.getProductId())))
                )
                // 4. Collect the results of all the async calls into a single List of Tuples.
                .collectList()
                // 5. Once all product details are fetched, proceed to build and save the order.
                .flatMap(productTuples -> {
                    // Create OrderItem objects using the trusted price from the product-service.
                    List<OrderItem> orderItems = productTuples.stream()
                            .map(tuple -> {
                                Product product = tuple.getT1();
                                Integer quantity = tuple.getT2();

                                OrderItem orderItem = new OrderItem();
                                orderItem.setProductId(product.getId());
                                orderItem.setQuantity(quantity);
                                orderItem.setPriceAtPurchase(product.getPrice()); // Use the trusted price
                                orderItem.setOrder(order);
                                return orderItem;
                            })
                            .collect(Collectors.toList());

                    order.setItems(orderItems);

                    // Calculate the total price based on the trusted data.
                    double totalPrice = orderItems.stream()
                            .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                            .sum();
                    order.setFinalAmount(totalPrice);

                    return Mono.fromCallable(() -> orderRepository.save(order))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSuccess(savedOrder -> {
                                try {
                                    outboxService.requestStockReservation(savedOrder);
                                } catch (Exception e) {
                                    log.error("Failed to create outbox event for order {}", savedOrder.getId(), e);
                                }
                            });
                });
    }
    // NEW: Kafka Listener to handle the result from ProductService
    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.stock-reservation-result}", groupId = "order-service-group")
    public void consumeStockReservationResult(StockReservationResponse response) {
        log.info("Received stock reservation result for order {}: {}", response.getOrderId(), response.getStatus());

        Order order = orderRepository.findByIdForUpdate(response.getOrderId())
                .orElseThrow(() -> new RuntimeException("Received stock result for non-existent order: " + response.getOrderId()));

        if (order.getCurrentStatus() == OrderStatus.ORDER_CANCELLED_PENDING) {
            log.warn("Stock result received for order {} that was already marked for cancellation.", order.getId());
            if ("SUCCESS".equals(response.getStatus())) {
                // The stock WAS reserved, so we must now revert it.
                triggerStockReversion(order);
                log.info("Triggering stock reversion for a successfully reserved but cancelled order.");
            }
            // Whether success or fail, the final status is CANCELLED.
            order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
        } else if (order.getCurrentStatus() == OrderStatus.ORDER_PENDING) {
            // Original logic for a normal flow
            if ("SUCCESS".equals(response.getStatus())) {
                order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);
            } else {
                order.setCurrentStatus(OrderStatus.ORDER_REJECTED);
            }
        } else {
            log.warn("Received stock result for order {} in an unexpected state: {}. Ignoring.", order.getId(), order.getCurrentStatus());
            return;
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getCustomerId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not own this order.");
        }

        if (order.getCurrentStatus() == OrderStatus.ORDER_PENDING) {
            order.setCurrentStatus(OrderStatus.ORDER_CANCELLED_PENDING); // A new state
            orderRepository.save(order);
            log.info("Order {} marked for cancellation. Waiting for stock reservation result.", orderId);
        } else if (order.getCurrentStatus() == OrderStatus.ORDER_CONFIRMED) {
            triggerStockReversion(order);
            order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
            orderRepository.save(order);
            log.info("Scheduled stock reversion for cancelled order {}", orderId);
        } else {
            throw new IllegalStateException("Order cannot be cancelled in its current state: " + order.getCurrentStatus());
        }
    }

    // Other methods...
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findAllByCustomerId(userId);
    }

    public Order getOrderByIdAndUserId(UUID orderId, UUID userId) {
        return orderRepository.findAllByCustomerIdAndId(userId, orderId)
                .orElseThrow(() -> new RuntimeException("Order not found or you do not have permission to view it."));
    }

    public List<Order> getOrdersByMerchantId(UUID merchantId) {
        return orderRepository.findAllByMerchantId(merchantId);
    }
}