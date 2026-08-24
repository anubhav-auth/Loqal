package com.loqal.platform.controller;

import com.loqal.platform.onboarding.MerchantOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Merchant self-service storefront profile (PRD §8.3). */
@RestController
@RequestMapping("/platform/merchant/profile")
@RequiredArgsConstructor
public class StorefrontProfileController {

    private final MerchantOnboardingService onboardingService;

    @GetMapping
    public Mono<ResponseEntity<com.loqal.platform.entity.MerchantProfile>> getOwn(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        String email = jwt.getClaimAsString("sub");
        return onboardingService.getOrCreateOwnProfile(tenantId, userId, email)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping
    public Mono<ResponseEntity<com.loqal.platform.entity.MerchantProfile>> updateOwn(@RequestBody com.loqal.platform.entity.MerchantProfile updates,
                                                  @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return onboardingService.updateOwnProfile(tenantId, updates)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.notFound().build()));
    }
}
