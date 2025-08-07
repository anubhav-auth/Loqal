package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.OrderStatus;
import com.Loqal.orderservice.dto.ProductOrderRequest;
import com.Loqal.orderservice.dto.events.*;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.entity.OrderItem;
import com.Loqal.orderservice.entity.Product;
import com.Loqal.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {


    private final OrderRepository orderRepository;
    private final WebClient productServiceWebClient;
    private final WebClient paymentServiceWebClient;
    private final KafkaTemplate<String, RefundRequestedEvent> kafkaTemplate;
    private final OutboxService outboxService;

    @Value("${spring.kafka.topic.order-cancel}")
    private String orderCancellationTopic;

    public OrderService(
            OrderRepository orderRepository,
            @Qualifier("productServiceWebClient") WebClient productServiceWebClient,
            @Qualifier("paymentServiceWebClient") WebClient paymentServiceWebClient,
            KafkaTemplate<String, RefundRequestedEvent> kafkaTemplate,
            OutboxService outboxService
    ) {
        this.orderRepository = orderRepository;
        this.productServiceWebClient = productServiceWebClient;
        this.paymentServiceWebClient = paymentServiceWebClient;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxService = outboxService;
    }

    @Value("${kafka.topic.refund-requested}")
    private String refundRequestedTopic;

    private record PaymentServiceRequest(String receipt, double amount) {}
    public record PaymentServiceResponse(UUID razorpayOrderId) {}
//    public record OrderCreationResponse(UUID orderId, UUID razorpayOrderId) {}


    private void triggerStockReversion(Order order) {
        List<ProductOrderRequest> itemsToRevert = order.getItems().stream()
                .map(item -> new ProductOrderRequest(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());
        OrderCancellationEvent event = new OrderCancellationEvent(order.getId(), itemsToRevert);
        outboxService.requestStockReversion(event);
    }

    @Transactional
    public Mono<OrderCreationResponse> createOrder(OrderRequest req, UUID userId) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(userId);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        // The initial status is now PENDING_PAYMENT.
        order.setCurrentStatus(OrderStatus.ORDER_PAYMENT_PENDING);

        // This block is preserved from your original code to fetch product details and prices.
        return Flux.fromIterable(req.getItems())
                .flatMap(itemReq ->
                        productServiceWebClient.get()
                                .uri("/api/products/{id}", itemReq.getProductId()) // Assuming endpoint
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
                                orderItem.setOrderId(order.getId());
                                return orderItem;
                            }).collect(Collectors.toList());

                    order.setItems(orderItems);

                    double totalPrice = orderItems.stream()
                            .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                            .sum();
                    order.setFinalAmount(totalPrice);

                    return orderRepository.save(order);
                })
                .flatMap(savedOrder -> {
                    log.info("Order {} created. Calling payment service to get payment link.", savedOrder.getId());
                    var paymentRequest = new PaymentServiceRequest(savedOrder.getId().toString(), savedOrder.getFinalAmount());

                    return paymentServiceWebClient.post()
                            .uri("/api/payments/order")
                            .bodyValue(paymentRequest)
                            .retrieve()
                            .bodyToMono(PaymentServiceResponse.class)
                            .flatMap(paymentResponse -> {
                                savedOrder.setRazorpayOrderId(paymentResponse.razorpayOrderId());
                                return orderRepository.save(savedOrder); // Save Razorpay Order ID for reference
                            })
                            .map(finalOrder -> new OrderCreationResponse(finalOrder.getId(), finalOrder.getRazorpayOrderId()));
                });
    }

    @Transactional
    @KafkaListener(topics = "${kafka.topic.payment-completed}", groupId = "order-service-group")
    public void consumePaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Payment completed for order {}. Updating status and requesting stock reservation.", event.orderId());
        orderRepository.findById(event.orderId())
                .flatMap(order -> {
                    if (order.getCurrentStatus() != OrderStatus.ORDER_PAYMENT_PENDING) {
                        log.warn("Payment event for order {} already processed. Current State: {}", order.getId(), order.getCurrentStatus());
                        return Mono.empty(); // Idempotency
                    }
                    order.setCurrentStatus(OrderStatus.ORDER_PAYMENT_COMPLETE);
                    order.setRazorpayPaymentId(event.razorpayPaymentId()); // Save for potential refunds
                    order.setUpdatedAt(LocalDateTime.now());

                    return orderRepository.save(order)
                            .publishOn(Schedulers.boundedElastic())
                            .doOnSuccess(finalOrder -> {
                                log.info("Order {} is paid. Using OutboxService to request stock reservation.", finalOrder.getId());
                                outboxService.requestStockReservation(finalOrder);
                            });
                }).subscribe();
    }

    @Transactional
    @KafkaListener(topics = "${kafka.topic.stock-reservation-result}", groupId = "order-service-group")
    public void consumeStockReservationResult(StockReservationResponse response) {
        log.info("Received stock reservation result for order {}: {}", response.getOrderId(), response.getStatus());

        orderRepository.findById(response.getOrderId())
                .flatMap(order -> {
                    // This handles a race condition where a user cancels while stock reservation is in-flight.
                    if (order.getCurrentStatus() == OrderStatus.ORDER_CANCELLED_PENDING) {
                        log.warn("Stock result received for order {} that was already marked for cancellation.", order.getId());
                        if ("SUCCESS".equals(response.getStatus())) {
                            triggerStockReversion(order); // Revert the stock that was just reserved.
                        }
                        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
                        return orderRepository.save(order);
                    }

                    // This is the main success/failure path.
                    if (order.getCurrentStatus() != OrderStatus.ORDER_PAYMENT_COMPLETE) {
                        log.warn("Received stock result for order {} in an unexpected state: {}. Ignoring message.", order.getId(), order.getCurrentStatus());
                        return Mono.empty();
                    }

                    if ("SUCCESS".equals(response.getStatus())) {
                        order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);
                        log.info("Order {} confirmed.", order.getId());
                    } else {
                        // CHANGE: This is the "puncture-proof" part. If stock fails, we trigger a refund.
                        order.setCurrentStatus(OrderStatus.ORDER_REJECTED);
                        log.error("Stock reservation FAILED for paid order {}. Triggering refund.", order.getId());
                        var refundEvent = new RefundRequestedEvent(
                                order.getId(),
                                order.getRazorpayPaymentId(),
                                order.getFinalAmount()
                        );
                        kafkaTemplate.send(refundRequestedTopic, refundEvent);
                    }
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .doOnError(e -> log.error("Error processing stock reservation result for order {}. The message will be retried.", response.getOrderId(), e))
                .subscribe();
    }

    @Transactional
    public Mono<Void> cancelOrder(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found: " + orderId)))
                .flatMap(order -> {
                    if (!order.getCustomerId().equals(userId)) {
                        return Mono.error(new SecurityException("Unauthorized: You do not own this order."));
                    }

                    switch (order.getCurrentStatus()) {
                        case ORDER_PAYMENT_PENDING:
                            // Can be cancelled directly before payment.
                            order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
                            log.info("Order {} cancelled before payment.", orderId);
                            break;
                        case ORDER_PAYMENT_COMPLETE:
                            // Payment is done, but stock reservation might be in-flight. Mark for cancellation.
                            order.setCurrentStatus(OrderStatus.ORDER_CANCELLED_PENDING);
                            log.info("Order {} marked for cancellation post-payment. Waiting for stock result.", orderId);
                            break;
                        case ORDER_CONFIRMED:
                            // Order is fully confirmed, must revert stock.
                            triggerStockReversion(order);
                            order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
                            log.info("Scheduled stock reversion for confirmed-and-cancelled order {}", orderId);
                            break;
                        default:
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