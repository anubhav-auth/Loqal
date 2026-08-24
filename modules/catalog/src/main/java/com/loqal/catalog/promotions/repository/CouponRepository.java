package com.loqal.catalog.promotions.repository;

import com.loqal.catalog.promotions.entity.Coupon;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CouponRepository extends R2dbcRepository<Coupon, UUID> {

    Mono<Coupon> findByTenantIdAndCode(UUID tenantId, String code);

    Flux<Coupon> findAllByTenantId(UUID tenantId);
}
