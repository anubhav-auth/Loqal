package com.loqal.catalog.promotions;

import com.loqal.catalog.promotions.entity.Coupon;
import com.loqal.catalog.promotions.entity.CouponRedemption;
import com.loqal.catalog.promotions.repository.CouponRedemptionRepository;
import com.loqal.catalog.promotions.repository.CouponRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PromotionService implements PromotionApi {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    public PromotionService(CouponRepository couponRepository, CouponRedemptionRepository redemptionRepository) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
    }

    // ---------- merchant management ----------

    public Mono<Coupon> createCoupon(UUID tenantId, Coupon coupon) {
        coupon.setTenantId(tenantId);
        coupon.setId(UUID.randomUUID());
        coupon.markNew();
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setTimesUsed(0);
        coupon.setActive(true);
        return couponRepository.findByTenantIdAndCode(tenantId, coupon.getCode())
                .flatMap(existing -> Mono.error(new IllegalArgumentException(
                        "Coupon code already exists for this merchant")))
                .switchIfEmpty(Mono.defer(() -> {
                    validateShape(coupon);
                    return couponRepository.save(coupon);
                }))
                .cast(Coupon.class);
    }

    public Flux<Coupon> listCoupons(UUID tenantId) {
        return couponRepository.findAllByTenantId(tenantId);
    }

    /** Soft-deactivation; history is preserved via redemptions. */
    public Mono<Coupon> deactivate(UUID tenantId, UUID couponId) {
        return couponRepository.findById(couponId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Coupon not found")))
                .flatMap(coupon -> {
                    coupon.setActive(false);
                    coupon.setUpdatedAt(LocalDateTime.now());
                    return couponRepository.save(coupon);
                });
    }

    // ---------- checkout-time validation (PRD §8.1) ----------

    @Override
    public Mono<DiscountResult> validateCoupon(UUID tenantId, UUID userId, UUID orderId,
                                               String code, long subtotalMinor) {
        if (code == null || code.isBlank()) {
            return Mono.error(new IllegalArgumentException("Coupon code is required"));
        }
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByTenantIdAndCode(tenantId, code.trim())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid coupon code")))
                .filter(Coupon::isActive)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Coupon is no longer active")))
                .filter(c -> c.getValidFrom() == null || !c.getValidFrom().isAfter(now))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Coupon is not valid yet")))
                .filter(c -> c.getValidUntil() == null || !c.getValidUntil().isBefore(now))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Coupon has expired")))
                .filter(c -> subtotalMinor >= c.getMinOrderValueMinor())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Order does not meet the minimum value for this coupon")))
                .filter(c -> c.getUsageLimitGlobal() == null || c.getTimesUsed() < c.getUsageLimitGlobal())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Coupon usage limit reached")))
                .flatMap(c -> enforcePerUserLimit(c, userId)
                        .thenReturn(computeDiscount(c, subtotalMinor))
                        .flatMap(discountMinor -> recordRedemption(c, userId, orderId, discountMinor)));
    }

    // ---------- internals ----------

    private Mono<DiscountResult> recordRedemption(Coupon coupon, UUID userId, UUID orderId, long discountMinor) {
        CouponRedemption redemption = new CouponRedemption();
        redemption.setId(UUID.randomUUID());
        redemption.setCouponId(coupon.getId());
        redemption.setTenantId(coupon.getTenantId());
        redemption.setUserId(userId);
        redemption.setOrderId(orderId);
        redemption.setDiscountMinor(discountMinor);
        redemption.setRedeemedAt(LocalDateTime.now());

        return redemptionRepository.save(redemption)
                .then(Mono.defer(() -> redemptionRepository.countByCouponId(coupon.getId())
                        .defaultIfEmpty(0L)
                        .map(count -> count.intValue())))
                .doOnNext(count -> {
                    coupon.setTimesUsed(count);
                    coupon.setUpdatedAt(LocalDateTime.now());
                })
                .then(couponRepository.save(coupon))
                .map(saved -> new DiscountResult(saved.getId(), saved.getCode(), discountMinor));
    }

    private Mono<Coupon> enforcePerUserLimit(Coupon coupon, UUID userId) {
        if (coupon.getUsageLimitPerUser() == null) {
            return Mono.just(coupon);
        }
        return redemptionRepository.findByCouponIdAndUserId(coupon.getId(), userId)
                .count()
                .flatMap(userUses -> userUses < coupon.getUsageLimitPerUser()
                        ? Mono.just(coupon)
                        : Mono.error(new IllegalArgumentException("You have already used this coupon")));
    }

    private long computeDiscount(Coupon coupon, long subtotalMinor) {
        long discount;
        if (Coupon.TYPE_PERCENT.equals(coupon.getDiscountType())) {
            discount = Math.round(subtotalMinor * coupon.getValue() / 10_000.0); // basis points
        } else if (Coupon.TYPE_FIXED.equals(coupon.getDiscountType())) {
            discount = coupon.getValue();
        } else {
            throw new IllegalArgumentException("Unknown coupon type");
        }
        if (coupon.getMaxDiscountMinor() != null && discount > coupon.getMaxDiscountMinor()) {
            discount = coupon.getMaxDiscountMinor();
        }
        return Math.min(discount, subtotalMinor); // never exceed subtotal
    }


    private void validateShape(Coupon coupon) {
        boolean ok = switch (safeType(coupon)) {
            case Coupon.TYPE_PERCENT ->
                    coupon.getValue() > 0 && coupon.getValue() <= 10_000; // ≤100%
            case Coupon.TYPE_FIXED -> coupon.getValue() > 0;
            default -> false;
        };
        if (!ok) {
            throw new IllegalArgumentException("Invalid coupon configuration");
        }
    }

    private static String safeType(Coupon coupon) {
        return coupon.getDiscountType() == null ? "" : coupon.getDiscountType();
    }
}
