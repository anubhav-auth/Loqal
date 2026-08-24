package com.loqal.catalog.promotions;

import com.loqal.catalog.promotions.entity.Coupon;
import com.loqal.catalog.promotions.entity.CouponRedemption;
import com.loqal.catalog.promotions.repository.CouponRedemptionRepository;
import com.loqal.catalog.promotions.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    private CouponRepository couponRepository;
    private CouponRedemptionRepository redemptionRepository;
    private PromotionService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        redemptionRepository = mock(CouponRedemptionRepository.class);
        when(redemptionRepository.save(any())).thenReturn(Mono.empty());
        when(redemptionRepository.countByCouponId(any())).thenReturn(Mono.just(1L));
        service = new PromotionService(couponRepository, redemptionRepository);
    }

    private Coupon coupon(String type, long value) {
        Coupon c = new Coupon();
        c.setId(UUID.randomUUID());
        c.setTenantId(tenantId);
        c.setCode("SAVE10");
        c.setDiscountType(type);
        c.setValue(value);
        c.setMinOrderValueMinor(0);
        c.setValidFrom(LocalDateTime.now().minusDays(1));
        c.setValidUntil(LocalDateTime.now().plusDays(1));
        c.setActive(true);
        return c;
    }

    private void stubFind(Coupon c) {
        when(couponRepository.findByTenantIdAndCode(any(), anyString())).thenReturn(Mono.just(c));
        when(couponRepository.findById(org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(Mono.just(c));
        when(couponRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    void percentDiscountComputedInBasisPoints() {
        stubFind(coupon(Coupon.TYPE_PERCENT, 1000)); // 10%

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .assertNext(result -> assertEquals(5_000, result.discountMinor())) // 10% of ₹500
                .verifyComplete();
    }

    @Test
    void fixedDiscountApplied() {
        stubFind(coupon(Coupon.TYPE_FIXED, 7_500));

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .assertNext(result -> assertEquals(7_500, result.discountMinor()))
                .verifyComplete();
    }

    @Test
    void discountCappedAtMax() {
        Coupon c = coupon(Coupon.TYPE_PERCENT, 5000); // 50%
        c.setMaxDiscountMinor(4_000L);
        stubFind(c);

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .assertNext(result -> assertEquals(4_000, result.discountMinor()))
                .verifyComplete();
    }

    @Test
    void discountNeverExceedsSubtotal() {
        stubFind(coupon(Coupon.TYPE_FIXED, 99_000));

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .assertNext(result -> assertEquals(50_000, result.discountMinor()))
                .verifyComplete();
    }

    @Test
    void expiredCouponRejected() {
        Coupon c = coupon(Coupon.TYPE_PERCENT, 1000);
        c.setValidUntil(LocalDateTime.now().minusHours(1));
        stubFind(c);

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void minOrderValueEnforced() {
        Coupon c = coupon(Coupon.TYPE_PERCENT, 1000);
        c.setMinOrderValueMinor(100_000);
        stubFind(c);

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void globalUsageLimitEnforced() {
        Coupon c = coupon(Coupon.TYPE_PERCENT, 1000);
        c.setUsageLimitGlobal(5);
        c.setTimesUsed(5);
        stubFind(c);

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void perUserLimitEnforced() {
        Coupon c = coupon(Coupon.TYPE_PERCENT, 1000);
        c.setUsageLimitPerUser(1);
        stubFind(c);
        when(redemptionRepository.findByCouponIdAndUserId(any(), any()))
                .thenReturn(Flux.just(new CouponRedemption()));

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void inactiveCouponRejected() {
        Coupon c = coupon(Coupon.TYPE_PERCENT, 1000);
        c.setActive(false);
        stubFind(c);

        StepVerifier.create(service.validateCoupon(tenantId, userId, UUID.randomUUID(), "SAVE10", 50_000))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void duplicateCodeRejectedOnCreate() {
        when(couponRepository.findByTenantIdAndCode(any(), anyString()))
                .thenReturn(Mono.just(new Coupon()));

        StepVerifier.create(service.createCoupon(tenantId, coupon(Coupon.TYPE_PERCENT, 1000)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
