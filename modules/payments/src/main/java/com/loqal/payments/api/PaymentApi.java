package com.loqal.payments.api;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Published API of the payments module. Other modules must depend only on this
 * interface — never on internal services, repositories or entities.
 */
public interface PaymentApi {

    /**
     * Creates a Razorpay order plus the local payment record.
     *
     * @param orderId    local order id (used as Razorpay receipt reference)
     * @param tenantId   merchant tenant, may be null for customer-direct flows
     * @param amountMinor integer minor units (paise)
     * @param currency   ISO-4217 code
     */
    Mono<PaymentInitiation> createPayment(UUID orderId, UUID tenantId, long amountMinor, String currency);

    record PaymentInitiation(UUID paymentId, String razorpayOrderId, long amountMinor, String currency) {
    }
}
