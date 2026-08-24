package com.loqal.catalog.promotions.repository;

import com.loqal.catalog.promotions.entity.CouponRedemption;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CouponRedemptionRepository extends R2dbcRepository<CouponRedemption, UUID> {

    Flux<CouponRedemption> findByCouponIdAndUserId(UUID couponId, UUID userId);

    Mono<Long> countByCouponId(UUID couponId);
}
