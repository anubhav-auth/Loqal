package com.loqal.payments.gateway;

import reactor.core.publisher.Mono;

/**
 * Provider seam (PRD Phase 3): additional payment providers plug in here
 * without touching PaymentService or callers.
 */
public interface PaymentProvider {

    /** Provider identifier used for configuration selection. */
    String name();

    /** Creates a provider-side order; returns the provider order id. */
    Mono<String> createOrder(String receipt, long amountMinor, String currency);

    /** Initiates a refund; returns the provider refund id. */
    Mono<String> refund(String providerPaymentId, long amountMinor);

    /** Verifies an inbound webhook payload against the provider's scheme. */
    boolean verifyWebhookSignature(String payload, String signature);
}
