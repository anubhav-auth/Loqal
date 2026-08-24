package com.loqal.payments.gateway;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Bridges the blocking Razorpay Java SDK onto virtual threads so no
 * Reactor event loop is ever blocked (PRD PAY-106).
 */
@Component
@Slf4j
public class RazorpayGateway implements PaymentProvider {

    @Override
    public String name() {
        return "razorpay";
    }

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(15);

    private final Scheduler virtualThreadScheduler =
            Schedulers.fromExecutorService(Executors.newVirtualThreadPerTaskExecutor(), "razorpay-vt");

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayGateway(@Value("${razorpay.key-id:}") String keyId,
                           @Value("${razorpay.key-secret:}") String keySecret,
                           @Value("${razorpay.webhook-secret:}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public Mono<String> createOrder(String receipt, long amountMinor, String currency) {
        return Mono.fromCallable(() -> {
                    JSONObject orderRequest = new JSONObject();
                    orderRequest.put("amount", amountMinor);
                    orderRequest.put("currency", currency);
                    orderRequest.put("receipt", receipt);
                    RazorpayClient client = new RazorpayClient(keyId, keySecret);
                    return (String) client.orders.create(orderRequest).get("id");
                })
                .subscribeOn(virtualThreadScheduler)
                .timeout(CALL_TIMEOUT)
                .doOnError(e -> log.error("Razorpay createOrder failed for receipt {}: {}", receipt, e.getMessage()));
    }

    public Mono<String> refund(String razorpayPaymentId, long amountMinor) {
        return Mono.fromCallable(() -> {
                    JSONObject refundRequest = new JSONObject();
                    refundRequest.put("amount", amountMinor);
                    RazorpayClient client = new RazorpayClient(keyId, keySecret);
                    var refund = client.payments.refund(razorpayPaymentId, refundRequest);
                    return (String) refund.get("id");
                })
                .subscribeOn(virtualThreadScheduler)
                .timeout(CALL_TIMEOUT)
                .doOnError(e -> log.error("Razorpay refund failed for payment {}: {}", razorpayPaymentId, e.getMessage()));
    }

    /** HMAC-SHA256 verification of webhook payloads per Razorpay docs. */
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : expected) {
                hex.append(String.format("%02x", b));
            }
            return constantTimeEquals(hex.toString(), signature);
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
