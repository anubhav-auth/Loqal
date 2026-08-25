package com.loqal.orders.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.catalog.api.ProductApi;
import com.loqal.catalog.promotions.PromotionApi;
import com.loqal.contracts.events.RefundRequestedEvent;
import com.loqal.contracts.exception.InvalidCouponException;
import com.loqal.orders.dto.OrderRequest;
import com.loqal.orders.dto.OrderStatus;
import com.loqal.orders.dto.ProductOrderRequest;
import com.loqal.orders.dto.events.OrderCreationResponse;
import com.loqal.orders.entity.Order;
import com.loqal.orders.entity.OrderItem;
import com.loqal.orders.repository.OrderRepository;
import com.loqal.payments.api.PaymentApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductApi productApi;
    private PaymentApi paymentApi;
    private PromotionApi promotionApi;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OutboxService outboxService;
    private ObjectMapper objectMapper;
    private OrderService orderService;

    private final UUID userId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        orderRepository = mock(OrderRepository.class);
        productApi = mock(ProductApi.class);
        paymentApi = mock(PaymentApi.class);
        promotionApi = mock(PromotionApi.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        outboxService = mock(OutboxService.class);
        objectMapper = new ObjectMapper();

        orderService = new OrderService(
                orderRepository, productApi, paymentApi, promotionApi,
                kafkaTemplate, outboxService, objectMapper
        );

        // Set @Value fields via reflection
        var topicField = OrderService.class.getDeclaredField("orderCancellationTopic");
        topicField.setAccessible(true);
        topicField.set(orderService, "order-cancel");

        var refundTopicField = OrderService.class.getDeclaredField("refundRequestedTopic");
        refundTopicField.setAccessible(true);
        refundTopicField.set(orderService, "refund-requested");

        // Common stubs
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private OrderRequest buildOrderRequest(String couponCode) {
        OrderRequest req = new OrderRequest();
        req.setMerchantId(merchantId);
        req.setCouponCode(couponCode);
        ProductOrderRequest item = new ProductOrderRequest(productId, 2);
        req.setItems(List.of(item));
        return req;
    }

    private Order buildOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setCustomerId(userId);
        order.setMerchantId(merchantId);
        order.setCurrentStatus(status);
        order.setTotalAmountMinor(1000L);
        order.setFinalAmountMinor(1000L);
        order.setDiscountAmountMinor(0L);
        order.setRazorpayPaymentId("pay_abc123");
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(2);
        item.setPriceAtPurchaseMinor(500L);
        order.setItems(List.of(item));
        return order;
    }

    // ── createOrder ──

    @Test
    void createOrder_success_noCoupon() {
        OrderRequest req = buildOrderRequest(null);
        ProductApi.ProductPrice price = new ProductApi.ProductPrice(productId, 500L, 10, true);
        PaymentApi.PaymentInitiation payInit = new PaymentApi.PaymentInitiation(paymentId, "order_razor123", 1000L, "INR");

        when(productApi.findPrice(productId)).thenReturn(Mono.just(price));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(paymentApi.createPayment(any(), any(), anyLong(), anyString())).thenReturn(Mono.just(payInit));

        StepVerifier.create(orderService.createOrder(req, userId))
                .assertNext(resp -> {
                    assertNotNull(resp.orderId());
                    assertEquals("order_razor123", resp.razorpayOrderId());
                })
                .verifyComplete();

        verify(orderRepository, times(3)).save(any(Order.class));
        verify(paymentApi).createPayment(any(), isNull(), eq(1000L), eq("INR"));
        verify(promotionApi, never()).validateCoupon(any(), any(), any(), any(), anyLong());
    }

    @Test
    void createOrder_withCoupon_appliesDiscount() {
        OrderRequest req = buildOrderRequest("SAVE10");
        ProductApi.ProductPrice price = new ProductApi.ProductPrice(productId, 500L, 10, true);
        PromotionApi.DiscountResult discount = new PromotionApi.DiscountResult(UUID.randomUUID(), "SAVE10", 100L);
        PaymentApi.PaymentInitiation payInit = new PaymentApi.PaymentInitiation(paymentId, "order_razor456", 900L, "INR");

        when(productApi.findPrice(productId)).thenReturn(Mono.just(price));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promotionApi.validateCoupon(eq(merchantId), eq(userId), any(), eq("SAVE10"), eq(1000L)))
                .thenReturn(Mono.just(discount));
        when(paymentApi.createPayment(any(), any(), anyLong(), anyString())).thenReturn(Mono.just(payInit));

        StepVerifier.create(orderService.createOrder(req, userId))
                .assertNext(resp -> {
                    assertNotNull(resp.orderId());
                    assertEquals("order_razor456", resp.razorpayOrderId());
                })
                .verifyComplete();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(3)).save(captor.capture());
        Order savedOrder = captor.getAllValues().get(2);
        assertEquals(100L, savedOrder.getDiscountAmountMinor());
        assertEquals(900L, savedOrder.getFinalAmountMinor());
    }

    @Test
    void createOrder_invalidCoupon_propagatesException() {
        OrderRequest req = buildOrderRequest("BADCOUPON");
        ProductApi.ProductPrice price = new ProductApi.ProductPrice(productId, 500L, 10, true);

        when(productApi.findPrice(productId)).thenReturn(Mono.just(price));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promotionApi.validateCoupon(any(), any(), any(), eq("BADCOUPON"), anyLong()))
                .thenReturn(Mono.error(new IllegalArgumentException("coupon not found")));

        StepVerifier.create(orderService.createOrder(req, userId))
                .expectError(InvalidCouponException.class)
                .verify();
    }

    // ── cancelOrder ──

    @Test
    void cancelOrder_paymentPending_cancelsDirectly() {
        Order order = buildOrder(OrderStatus.ORDER_PAYMENT_PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.cancelOrder(orderId, userId))
                .verifyComplete();

        assertEquals(OrderStatus.ORDER_CANCELLED, order.getCurrentStatus());
        verify(outboxService, never()).requestStockReversion(any());
    }

    @Test
    void cancelOrder_paymentComplete_marksCancellationPending() {
        Order order = buildOrder(OrderStatus.ORDER_PAYMENT_COMPLETE);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.cancelOrder(orderId, userId))
                .verifyComplete();

        assertEquals(OrderStatus.ORDER_CANCELLED_PENDING, order.getCurrentStatus());
    }

    @Test
    void cancelOrder_confirmed_triggersStockReversion() {
        Order order = buildOrder(OrderStatus.ORDER_CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.cancelOrder(orderId, userId))
                .verifyComplete();

        assertEquals(OrderStatus.ORDER_CANCELLED, order.getCurrentStatus());
        verify(outboxService).requestStockReversion(any());
    }

    @Test
    void cancelOrder_wrongUser_throwsSecurityException() {
        Order order = buildOrder(OrderStatus.ORDER_PAYMENT_PENDING);
        order.setCustomerId(UUID.randomUUID());

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrder(orderId, userId))
                .expectError(SecurityException.class)
                .verify();
    }

    @Test
    void cancelOrder_deliveredOrder_throwsIllegalState() {
        Order order = buildOrder(OrderStatus.ORDER_DELIVERED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrder(orderId, userId))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void cancelOrder_orderNotFound_throwsRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.cancelOrder(orderId, userId))
                .expectError(RuntimeException.class)
                .verify();
    }

    // ── returnOrder ──

    @Test
    void returnOrder_confirmedOrder_sendsRefundAndSetsPending() {
        Order order = buildOrder(OrderStatus.ORDER_CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.returnOrder(orderId, userId))
                .verifyComplete();

        assertEquals(OrderStatus.ORDER_CANCELLED_PENDING, order.getCurrentStatus());
        verify(kafkaTemplate).send(eq("refund-requested"), anyString());
    }

    @Test
    void returnOrder_deliveredOrder_succeeds() {
        Order order = buildOrder(OrderStatus.ORDER_DELIVERED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.returnOrder(orderId, userId))
                .verifyComplete();

        assertEquals(OrderStatus.ORDER_CANCELLED_PENDING, order.getCurrentStatus());
    }

    @Test
    void returnOrder_wrongUser_throwsSecurityException() {
        Order order = buildOrder(OrderStatus.ORDER_CONFIRMED);
        order.setCustomerId(UUID.randomUUID());

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.returnOrder(orderId, userId))
                .expectError(SecurityException.class)
                .verify();
    }

    @Test
    void returnOrder_paymentPending_throwsIllegalState() {
        Order order = buildOrder(OrderStatus.ORDER_PAYMENT_PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.returnOrder(orderId, userId))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void returnOrder_orderNotFound_throwsRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.returnOrder(orderId, userId))
                .expectError(RuntimeException.class)
                .verify();
    }
}
