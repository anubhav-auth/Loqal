package com.loqal.payments.service;

import com.loqal.contracts.events.PaymentCompletedEvent;
import com.loqal.contracts.events.RefundRequestedEvent;
import com.loqal.contracts.events.Topics;
import com.loqal.payments.api.PaymentApi;
import com.loqal.payments.entity.Payment;
import com.loqal.payments.entity.Refund;
import com.loqal.payments.gateway.RazorpayGateway;
import com.loqal.payments.repository.PaymentRepository;
import com.loqal.payments.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements PaymentApi {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RazorpayGateway razorpayGateway;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Mono<PaymentInitiation> createPayment(UUID orderId, UUID tenantId, long amountMinor, String currency) {
        String receipt = orderId.toString();
        return Mono.defer(() -> {
                    Payment payment = new Payment();
                    payment.setId(UUID.randomUUID());
                    payment.setTenantId(tenantId);
                    payment.setOrderId(orderId);
                    payment.setAmountMinor(amountMinor);
                    payment.setCurrency(currency);
                    payment.setStatus(Payment.STATUS_CREATED);
                    payment.setCreatedAt(LocalDateTime.now());
                    payment.setUpdatedAt(LocalDateTime.now());
                    return paymentRepository.save(payment);
                })
                .flatMap(payment -> razorpayGateway.createOrder(receipt, amountMinor, currency)
                        .flatMap(razorpayOrderId -> {
                            payment.setRazorpayOrderId(razorpayOrderId);
                            payment.setUpdatedAt(LocalDateTime.now());
                            return paymentRepository.save(payment);
                        })
                        .map(saved -> new PaymentInitiation(saved.getId(), saved.getRazorpayOrderId(),
                                saved.getAmountMinor(), saved.getCurrency())));
    }

    /** Webhook handler: signature already verified by the controller. */
    public Mono<Void> handleCapturedWebhook(String payload) {
        org.json.JSONObject root = new org.json.JSONObject(payload);
        org.json.JSONObject entity = root.getJSONObject("payload")
                .getJSONObject("payment").getJSONObject("entity");
        String razorpayPaymentId = entity.getString("id");
        String razorpayOrderId = entity.getString("order_id");
        long amountMinor = entity.getLong("amount");

        return paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No local payment for Razorpay order " + razorpayOrderId)))
                .flatMap(payment -> paymentRepository.existsByRazorpayPaymentId(razorpayPaymentId)
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                log.warn("Duplicate webhook for payment {}. Ignoring.", razorpayPaymentId);
                                return Mono.empty();
                            }
                            payment.setRazorpayPaymentId(razorpayPaymentId);
                            payment.setStatus(Payment.STATUS_CAPTURED);
                            payment.setUpdatedAt(LocalDateTime.now());
                            return paymentRepository.save(payment)
                                    .doOnSuccess(saved -> kafkaTemplate.send(Topics.PAYMENT_COMPLETED,
                                            new PaymentCompletedEvent(
                                                    saved.getOrderId(),
                                                    saved.getRazorpayOrderId(),
                                                    saved.getRazorpayPaymentId(),
                                                    "SUCCESS",
                                                    LocalDateTime.now())))
                                    .then();
                        }))
                .onErrorMap(IllegalStateException.class, e -> e)
                .then();
    }

    public Mono<Void> consumeRefundRequest(RefundRequestedEvent event) {
        log.info("Refund requested for order {}, payment {}: {} minor units",
                event.orderId(), event.razorpayPaymentId(), event.amountMinor());

        return paymentRepository.findByRazorpayPaymentId(event.razorpayPaymentId())
                
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No local payment found for provider id {}. Cannot refund.", event.razorpayPaymentId());
                    return Mono.empty();
                }))
                .flatMap(payment -> razorpayGateway.refund(payment.getRazorpayPaymentId(), event.amountMinor())
                        .flatMap(razorpayRefundId -> {
                            Refund refund = new Refund();
                            refund.setId(UUID.randomUUID());
                            refund.setTenantId(payment.getTenantId());
                            refund.setPaymentId(payment.getId());
                            refund.setRazorpayRefundId(razorpayRefundId);
                            refund.setAmountMinor(event.amountMinor());
                            refund.setStatus(Refund.STATUS_PROCESSED);
                            refund.setCreatedAt(LocalDateTime.now());
                            refund.setUpdatedAt(LocalDateTime.now());
                            return refundRepository.save(refund);
                        })
                        .doOnSuccess(refund -> log.info("Refund {} processed for order {}",
                                refund.getRazorpayRefundId(), event.orderId()))
                        .onErrorResume(e -> {
                            log.error("Refund failed for order {}: {}", event.orderId(), e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }
}
