package com.loqal.catalog.promotions.controller;

import com.loqal.catalog.promotions.PromotionService;
import com.loqal.catalog.promotions.entity.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/merchant/coupons")
    public Mono<ResponseEntity<Coupon>> createCoupon(@RequestBody Coupon coupon,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return promotionService.createCoupon(tenantId, coupon)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/merchant/coupons")
    public Flux<Coupon> listCoupons(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return promotionService.listCoupons(tenantId);
    }

    @DeleteMapping("/merchant/coupons/{couponId}")
    public Mono<ResponseEntity<Coupon>> deactivateCoupon(@PathVariable UUID couponId,
                                                         @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return promotionService.deactivate(tenantId, couponId)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.notFound().build()));
    }
}
