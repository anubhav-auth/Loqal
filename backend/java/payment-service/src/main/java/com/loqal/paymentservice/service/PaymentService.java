package com.loqal.paymentservice.service;

import com.loqal.paymentservice.entity.Payment;
import com.loqal.paymentservice.entity.events.PaymentCompletedEvent;
import com.loqal.paymentservice.entity.events.PaymentServiceRequest;
import com.loqal.paymentservice.entity.events.PaymentServiceResponse;
import com.loqal.paymentservice.entity.events.RefundRequestedEvent;
import com.loqal.paymentservice.repository.PaymentRepository;
import com.razorpay.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key.id}") private String keyId;
    @Value("${razorpay.key.secret}") private String keySecret;
    @Value("${razorpay.webhook.secret}") private String webhookSecret;
    @Value("${kafka.topic.payment-completed}") private String paymentCompletedTopic;

    private RazorpayClient getClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }

    public Mono<PaymentServiceResponse> createRazorpayOrder(PaymentServiceRequest request) {
        return Mono.fromCallable(() -> {
            JSONObject orderRequestJson = new JSONObject();
            orderRequestJson.put("amount", request.amount() * 100); // Amount in smallest currency unit
            orderRequestJson.put("currency", "INR");
            orderRequestJson.put("receipt", request.receipt());

            log.info("Creating Razorpay order for receipt: {}", request.receipt());
            Order order = getClient().orders.create(orderRequestJson);
            String razorpayOrderId = order.get("id");
            return new PaymentServiceResponse(razorpayOrderId);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> handleWebhook(String payload, String signature) {
        return verifyWebhookSignature(payload, signature)
                .filter(isValid -> isValid)
                .flatMap(isValid -> Mono.fromRunnable(() -> {
                    JSONObject jsonPayload = new JSONObject(payload);
                    String eventType = jsonPayload.getString("event");

                    if ("payment.captured".equals(eventType)) {
                        JSONObject paymentEntity = jsonPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                        processSuccessfulPayment(paymentEntity).subscribe();
                    } else {
                        log.info("Received unhandled Razorpay event type: {}", eventType);
                    }
                }));
    }

    private Mono<Void> processSuccessfulPayment(JSONObject paymentEntity) {
        String paymentId = paymentEntity.getString("id");
        return paymentRepository.findByRazorpayPaymentId(paymentId)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Payment with ID {} already processed. Ignoring duplicate webhook.", paymentId);
                        return Mono.empty();
                    }

                    Payment payment = new Payment();
                    payment.setId(UUID.randomUUID());
                    payment.setOrderId(UUID.fromString(paymentEntity.getString("receipt")));
                    payment.setRazorpayPaymentId(paymentId);
                    payment.setRazorpayOrderId(paymentEntity.getString("order_id"));
                    payment.setAmount(paymentEntity.getInt("amount") / 100.0);
                    payment.setCurrency(paymentEntity.getString("currency"));
                    payment.setStatus("CAPTURED");
                    payment.setCreatedAt(LocalDateTime.now());
                    payment.setUpdatedAt(LocalDateTime.now());

                    return paymentRepository.save(payment)
                            .doOnSuccess(savedPayment -> {
                                log.info("Payment {} saved. Publishing PaymentCompletedEvent.", savedPayment.getRazorpayPaymentId());
                                var event = new PaymentCompletedEvent(
                                        savedPayment.getOrderId(),
                                        savedPayment.getRazorpayOrderId(),
                                        savedPayment.getRazorpayPaymentId(),
                                        "SUCCESS",
                                        LocalDateTime.now()
                                );
                                kafkaTemplate.send(paymentCompletedTopic, event);
                            }).then();
                });
    }

    @KafkaListener(topics = "${kafka.topic.refund-requested}", groupId = "payment-service-group")
    public void consumeRefundRequest(RefundRequestedEvent event) {
        log.warn("Received refund request for payment ID: {}", event.razorpayPaymentId());
        processRefund(event.razorpayPaymentId(), event.amount()).subscribe();
    }

    private Mono<Void> processRefund(String paymentId, double amount) {
        return Mono.fromCallable(() -> {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount * 100);
            log.info("Attempting to refund payment ID: {}", paymentId);
            getClient().payments.refund(paymentId, refundRequest);
            log.info("Successfully processed refund for payment ID: {}", paymentId);
            // Here you could also update your local payment record status to "REFUNDED"
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Boolean> verifyWebhookSignature(String rawBody, String signature) {
        return Mono.fromCallable(() -> {
            try {
                Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
                return true;
            } catch (RazorpayException e) {
                log.error("Webhook signature verification failed!", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}