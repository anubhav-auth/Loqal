package com.loqal.payments.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic in-memory provider for tests, local development and CI.
 * Select via {@code payments.provider=mock}. Webhook signatures accept the
 * literal "mock-signature".
 */
@Component
@Slf4j
public class MockPaymentProvider implements PaymentProvider {

    public static final String MOCK_WEBHOOK_SIGNATURE = "mock-signature";
    private static final AtomicLong SEQ = new AtomicLong();

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public Mono<String> createOrder(String receipt, long amountMinor, String currency) {
        return Mono.fromCallable(() -> {
            String id = "mock_order_" + SEQ.incrementAndGet();
            log.info("MOCK createOrder receipt={} amountMinor={} -> {}", receipt, amountMinor, id);
            return id;
        });
    }

    @Override
    public Mono<String> refund(String providerPaymentId, long amountMinor) {
        return Mono.fromCallable(() -> {
            String id = "mock_refund_" + SEQ.incrementAndGet();
            log.info("MOCK refund payment={} amountMinor={} -> {}", providerPaymentId, amountMinor, id);
            return id;
        });
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        boolean valid = MOCK_WEBHOOK_SIGNATURE.equals(signature);
        if (valid) {
            log.debug("MOCK webhook accepted at {}", LocalDateTime.now());
        }
        return valid;
    }

    /** Helper for tests/CI: fabricate a plausible provider payment id. */
    public static String fakePaymentId() {
        return "mock_pay_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
