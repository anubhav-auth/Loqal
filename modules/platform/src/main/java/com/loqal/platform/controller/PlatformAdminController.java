package com.loqal.platform.controller;

import com.loqal.platform.audit.AuditService;
import com.loqal.platform.entity.AuditLog;
import com.loqal.platform.onboarding.MerchantOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Admin-only platform management endpoints (PRD §8.3).
 * Access control: /platform/admin/** requires ROLE_ADMIN (enforced in the
 * app-level security chain via the token's roles claim).
 */
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final MerchantOnboardingService onboardingService;
    private final AuditService auditService;

    @PostMapping("/admin/merchants/onboard")
    public Mono<ResponseEntity<Object>> onboard(@RequestBody MerchantOnboardingService.OnboardRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getClaimAsString("user_id"));
        String adminEmail = jwt.getClaimAsString("sub");
        return onboardingService.onboard(request, adminId, adminEmail)
                .map(profile -> ResponseEntity.status(HttpStatus.CREATED).<Object>body(profile))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()))))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()))));
    }

    @GetMapping("/admin/audit")
    public Flux<AuditLog> recentAudit(@RequestParam(defaultValue = "100") int limit) {
        return auditService.recent(limit);
    }

    record ErrorResponse(String message) {}
}
