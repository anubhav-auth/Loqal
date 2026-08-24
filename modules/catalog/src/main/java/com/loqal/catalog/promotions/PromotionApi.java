package com.loqal.catalog.promotions;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Published API of the promotions capability (PRD §8.1).
 * Checkout-time validation; orders module owns the discount snapshot.
 */
public interface PromotionApi {

    /**
     * Validates a coupon against the order context and returns the server-computed
     * discount in minor units.
     *
     * @param tenantId           merchant tenant whose coupon is being redeemed
     * @param userId             redeeming customer
     * @param orderId            the persisted order this redemption belongs to
     * @param code               coupon code
     * @param subtotalMinor      order subtotal before discount (sum of item snapshots)
     */
    Mono<DiscountResult> validateCoupon(UUID tenantId, UUID userId, UUID orderId,
                                        String code, long subtotalMinor);

    record DiscountResult(UUID couponId, String code, long discountMinor) {
    }
}
