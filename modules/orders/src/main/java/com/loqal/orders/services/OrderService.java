package com.loqal.orders.services;

import com.loqal.catalog.promotions.PromotionApi;
import com.loqal.payments.api.PaymentApi;
import com.loqal.catalog.api.ProductApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.orders.dto.OrderRequest;
import com.loqal.orders.dto.OrderStatus;
import com.loqal.contracts.events.ProductOrderRequest;
import com.loqal.contracts.events.OrderCancellationEvent;
import com.loqal.orders.dto.events.OrderCreationResponse;
import com.loqal.contracts.events.PaymentCompletedEvent;
import com.loqal.contracts.events.RefundRequestedEvent;
import com.loqal.contracts.events.StockReservationResponse;

import com.loqal.contracts.events.Topics;
import com.loqal.orders.entity.Order;
import com.loqal.orders.entity.OrderItem;
import com.loqal.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {


    private final OrderRepository orderRepository;
    private final ProductApi productApi;
    private final PaymentApi paymentApi;
    private final PromotionApi promotionApi;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.order-cancel}")
    private String orderCancellationTopic;

    public OrderService(
            OrderRepository orderRepository,
            ProductApi productApi,
            PaymentApi paymentApi,
            PromotionApi promotionApi,
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxService outboxService,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.productApi = productApi;
        this.paymentApi = paymentApi;
        this.promotionApi = promotionApi;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Value("${kafka.topic.refund-requested:refund-requested}")
    private String refundRequestedTopic;


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

        return Flux.fromIterable(req.getItems())
                .flatMap(itemReq ->
                        productApi.findPrice(itemReq.getProductId())
                                .map(productPrice -> Tuples.of(productPrice, itemReq.getQuantity()))
                )
                .collectList()
                .flatMap(productTuples -> {
                    List<OrderItem> orderItems = productTuples.stream()
                            .map(tuple -> {
                                ProductApi.ProductPrice product = tuple.getT1();
                                Integer quantity = tuple.getT2();
                                OrderItem orderItem = new OrderItem();
                                orderItem.setProductId(product.productId());
                                orderItem.setQuantity(quantity);
                                orderItem.setPriceAtPurchaseMinor(product.priceMinor());
                                orderItem.setOrderId(order.getId());
                                return orderItem;
                            }).collect(Collectors.toList());

                    order.setItems(orderItems);

                    long totalMinor = orderItems.stream()
                            .mapToLong(item -> item.getPriceAtPurchaseMinor() * item.getQuantity())
                            .sum();
                    order.setTotalAmountMinor(totalMinor);
                    order.setDiscountAmountMinor(0L);
                    order.setFinalAmountMinor(totalMinor);

                    return orderRepository.save(order)
                            .flatMap(persisted -> applyCoupon(req, userId, persisted, totalMinor)
                                    .defaultIfEmpty(0L)
                                    .flatMap(discountMinor -> {
                                        persisted.setDiscountAmountMinor(discountMinor);
                                        persisted.setFinalAmountMinor(totalMinor - discountMinor);
                                        return orderRepository.save(persisted);
                                    }));
                })
                .flatMap(savedOrder -> {
                    log.info("Order {} created. Requesting payment creation from payments module.", savedOrder.getId());
                    return paymentApi.createPayment(savedOrder.getId(), savedOrder.getMerchantId(), savedOrder.getFinalAmountMinor(), "INR")
                            .flatMap(paymentInitiation -> {
                                savedOrder.setRazorpayOrderId(paymentInitiation.razorpayOrderId());
                                return orderRepository.save(savedOrder);
                            })
                            .map(finalOrder -> new OrderCreationResponse(finalOrder.getId(), finalOrder.getRazorpayOrderId()));
                });
    }

    /**
     * Server-computed discount (PRD §8.1): client-supplied amounts are ignored.
     * A failed coupon validation aborts order creation with 422.
     */
    private Mono<Long> applyCoupon(OrderRequest req, UUID userId, Order persistedOrder, long subtotalMinor) {
        if (req.getCouponCode() == null || req.getCouponCode().isBlank()) {
            return Mono.empty();
        }
        UUID tenantId = req.getMerchantId();
        return promotionApi.validateCoupon(tenantId, userId, persistedOrder.getId(),
                        req.getCouponCode(), subtotalMinor)
                .map(PromotionApi.DiscountResult::discountMinor)
                .doOnSuccess(d -> log.info("Applied coupon {} worth {} minor units", req.getCouponCode(), d))
                .onErrorMap(IllegalArgumentException.class, e ->
                        new com.loqal.contracts.exception.InvalidCouponException(e.getMessage()));
    }

    /**
     * Post-confirmation return flow (PRD Phase 3): CONFIRMED/delivered orders
     * can be returned; a refund is requested and the order settles to
     * ORDER_RETURNED when the refund completes.
     */
    @Transactional
    public Mono<Void> returnOrder(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found: " + orderId)))
                .flatMap(order -> {
                    if (!order.getCustomerId().equals(userId)) {
                        return Mono.error(new SecurityException("Unauthorized: You do not own this order."));
                    }
                    switch (order.getCurrentStatus()) {
                        case ORDER_CONFIRMED:
                        case DELIVERY_ASSIGNED:
                        case ORDER_DISPATCHED:
                        case ORDER_DELIVERED:
                            break;
                        default:
                            return Mono.error(new IllegalStateException(
                                    "Order cannot be returned in its current state: " + order.getCurrentStatus()));
                    }
                    order.setCurrentStatus(OrderStatus.ORDER_CANCELLED_PENDING);
                    order.setUpdatedAt(LocalDateTime.now());
                    sendRefundRequest(new RefundRequestedEvent(
                            order.getId(),
                            order.getRazorpayPaymentId(),
                            order.getFinalAmountMinor()));
                    return orderRepository.save(order).then();
                });
    }

    @KafkaListener(topics = "${spring.kafka.topic.refund-completed}", groupId = Topics.GROUP_ORDERS)
    public void consumeRefundCompleted(String payload) {
        com.loqal.contracts.events.RefundCompletedEvent event =
                parse(payload, com.loqal.contracts.events.RefundCompletedEvent.class);
        if (event == null) return;
        log.info("Refund {} for order {}: {}", event.refundId(), event.orderId(), event.status());
        orderRepository.findById(event.orderId())
                .filter(o -> OrderStatus.ORDER_CANCELLED_PENDING.equals(o.getCurrentStatus()))
                .flatMap(order -> {
                    order.setCurrentStatus("PROCESSED".equals(event.status())
                            ? OrderStatus.ORDER_RETURNED
                            : OrderStatus.ORDER_REJECTED);
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order).then();
                })
                .subscribe();
    }

    @KafkaListener(topics = "${spring.kafka.topic.payment-completed}", groupId = Topics.GROUP_ORDERS)
    public void consumePaymentCompletedEvent(String payload) {
        PaymentCompletedEvent event = parse(payload, PaymentCompletedEvent.class);
        if (event == null) return;
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

    @KafkaListener(topics = "${spring.kafka.topic.stock-reservation-result}", groupId = Topics.GROUP_ORDERS)
    public void consumeStockReservationResult(String payload) {
        com.loqal.contracts.events.StockReservationResponse response = parse(payload, StockReservationResponse.class);
        if (response == null) return;
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
                                order.getFinalAmountMinor()
                        );
                        sendRefundRequest(refundEvent);
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

    private void sendRefundRequest(RefundRequestedEvent refundEvent) {
        try {
            kafkaTemplate.send(refundRequestedTopic, objectMapper.writeValueAsString(refundEvent));
        } catch (Exception e) {
            log.error("Failed to serialize refund request for order {}", refundEvent.orderId(), e);
        }
    }

    private <T> T parse(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception e) {
            log.error("Failed to deserialize {} payload: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
