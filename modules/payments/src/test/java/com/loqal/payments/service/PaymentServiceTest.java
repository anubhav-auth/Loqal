package com.loqal.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.contracts.events.RefundRequestedEvent;
import com.loqal.payments.entity.Payment;
import com.loqal.payments.entity.Refund;
import com.loqal.payments.gateway.PaymentProvider;
import com.loqal.payments.repository.PaymentRepository;
import com.loqal.payments.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.loqal.payments.api.PaymentApi.PaymentInitiation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private PaymentProvider paymentProvider;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private PaymentService service;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final UUID orderId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, objectMapper,
                refundRepository, paymentProvider, kafkaTemplate);
    }

    // ── createPayment ───────────────────────────────────────────────

    @Test
    void createPayment_success() {
        Payment payment = newPayment();
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(payment));
        when(paymentProvider.createOrder(eq(orderId.toString()), eq(5000L), eq("INR")))
                .thenReturn(Mono.just("order_abc123"));

        StepVerifier.create(service.createPayment(orderId, tenantId, 5000L, "INR"))
                .assertNext(init -> {
                    assertEquals(payment.getId(), init.paymentId());
                    assertEquals("order_abc123", init.razorpayOrderId());
                    assertEquals(5000L, init.amountMinor());
                    assertEquals("INR", init.currency());
                })
                .verifyComplete();

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    // ── handleCapturedWebhook ────────────────────────────────────────

    @Test
    void handleCapturedWebhook_success() {
        Payment payment = newPayment();
        payment.setRazorpayOrderId("order_abc123");
        payment.setRazorpayPaymentId(null);

        when(paymentRepository.findByRazorpayOrderId("order_abc123"))
                .thenReturn(Mono.just(payment));
        when(paymentRepository.existsByRazorpayPaymentId("pay_xyz789"))
                .thenReturn(Mono.just(false));
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(payment));
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(service.handleCapturedWebhook(webhookPayload()))
                .verifyComplete();

        verify(paymentRepository).save(argThat(p -> {
            Payment saved = (Payment) p;
            return "pay_xyz789".equals(saved.getRazorpayPaymentId())
                    && Payment.STATUS_CAPTURED.equals(saved.getStatus());
        }));
        verify(kafkaTemplate).send(eq("payment-completed"), anyString());
    }

    @Test
    void handleCapturedWebhook_duplicateIgnored() {
        Payment payment = newPayment();
        payment.setRazorpayOrderId("order_abc123");
        payment.setRazorpayPaymentId("pay_xyz789");

        when(paymentRepository.findByRazorpayOrderId("order_abc123"))
                .thenReturn(Mono.just(payment));
        when(paymentRepository.existsByRazorpayPaymentId("pay_xyz789"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.handleCapturedWebhook(webhookPayload()))
                .verifyComplete();

        verify(paymentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void handleCapturedWebhook_unknownOrder_throwsIllegalState() {
        when(paymentRepository.findByRazorpayOrderId("order_abc123"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.handleCapturedWebhook(webhookPayload()))
                .expectError(IllegalStateException.class)
                .verify();
    }

    // ── consumeRefundRequest ─────────────────────────────────────────

    @Test
    void consumeRefundRequest_success() {
        Payment payment = newPayment();
        payment.setRazorpayPaymentId("pay_xyz789");

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setRazorpayRefundId("rfnd_abc");
        refund.setAmountMinor(2000L);
        refund.setStatus(Refund.STATUS_PROCESSED);

        when(paymentRepository.findByRazorpayPaymentId("pay_xyz789"))
                .thenReturn(Mono.just(payment));
        when(paymentProvider.refund("pay_xyz789", 2000L))
                .thenReturn(Mono.just("rfnd_abc"));
        when(refundRepository.save(any(Refund.class))).thenReturn(Mono.just(refund));
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        var event = new RefundRequestedEvent(orderId, "pay_xyz789", 2000L);

        StepVerifier.create(service.consumeRefundRequest(event))
                .verifyComplete();

        verify(paymentProvider).refund("pay_xyz789", 2000L);
        verify(refundRepository).save(any(Refund.class));
        verify(kafkaTemplate).send(eq("refund-completed"), anyString());
    }

    @Test
    void consumeRefundRequest_noPayment_completesWithoutError() {
        when(paymentRepository.findByRazorpayPaymentId("pay_unknown"))
                .thenReturn(Mono.empty());

        var event = new RefundRequestedEvent(orderId, "pay_unknown", 1000L);

        StepVerifier.create(service.consumeRefundRequest(event))
                .verifyComplete();

        verify(paymentProvider, never()).refund(anyString(), anyLong());
        verify(refundRepository, never()).save(any());
    }

    @Test
    void consumeRefundRequest_providerError_completesWithoutError() {
        Payment payment = newPayment();
        payment.setRazorpayPaymentId("pay_xyz789");

        when(paymentRepository.findByRazorpayPaymentId("pay_xyz789"))
                .thenReturn(Mono.just(payment));
        when(paymentProvider.refund("pay_xyz789", 2000L))
                .thenReturn(Mono.error(new RuntimeException("provider down")));

        var event = new RefundRequestedEvent(orderId, "pay_xyz789", 2000L);

        StepVerifier.create(service.consumeRefundRequest(event))
                .verifyComplete();

        verify(refundRepository, never()).save(any());
    }

    // ── helpers ──────────────────────────────────────────────────────

    private Payment newPayment() {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setTenantId(tenantId);
        p.setOrderId(orderId);
        p.setAmountMinor(5000L);
        p.setCurrency("INR");
        p.setStatus(Payment.STATUS_CREATED);
        return p;
    }

    private String webhookPayload() {
        return "{\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_xyz789\",\"order_id\":\"order_abc123\",\"amount\":1999}}}}";
    }
}
