package com.loqal.platform.controller;

import com.loqal.platform.audit.AuditService;
import com.loqal.platform.entity.AuditLog;
import com.loqal.platform.entity.MerchantProfile;
import com.loqal.platform.onboarding.MerchantOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformAdminControllerTest {

    private MerchantOnboardingService onboardingService;
    private AuditService auditService;
    private PlatformAdminController controller;

    private final UUID adminId = UUID.randomUUID();
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        onboardingService = mock(MerchantOnboardingService.class);
        auditService = mock(AuditService.class);
        controller = new PlatformAdminController(onboardingService, auditService);
        jwt = Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("user_id", adminId.toString())
                .claim("sub", "admin@loqal.dev")
                .build();
    }

    private MerchantOnboardingService.OnboardRequest onboardRequest() {
        return new MerchantOnboardingService.OnboardRequest(
                UUID.randomUUID(), "Priya's Store", "Best store", null, null);
    }

    @Test
    void onboardReturns201OnSuccess() {
        MerchantProfile profile = new MerchantProfile();
        profile.setId(UUID.randomUUID());
        profile.setStoreName("Priya's Store");

        when(onboardingService.onboard(any(), eq(adminId), eq("admin@loqal.dev")))
                .thenReturn(Mono.just(profile));

        StepVerifier.create(controller.onboard(onboardRequest(), jwt))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertEquals("Priya's Store",
                            ((MerchantProfile) response.getBody()).getStoreName());
                })
                .verifyComplete();
    }

    @Test
    void onboardReturns400WhenUserNotFound() {
        when(onboardingService.onboard(any(), eq(adminId), eq("admin@loqal.dev")))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found")));

        StepVerifier.create(controller.onboard(onboardRequest(), jwt))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    PlatformAdminController.ErrorResponse body =
                            (PlatformAdminController.ErrorResponse) response.getBody();
                    assertEquals("User not found", body.message());
                })
                .verifyComplete();
    }

    @Test
    void onboardReturns409WhenDuplicateProfile() {
        when(onboardingService.onboard(any(), eq(adminId), eq("admin@loqal.dev")))
                .thenReturn(Mono.error(new IllegalStateException("Tenant already has a merchant profile")));

        StepVerifier.create(controller.onboard(onboardRequest(), jwt))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                    PlatformAdminController.ErrorResponse body =
                            (PlatformAdminController.ErrorResponse) response.getBody();
                    assertEquals("Tenant already has a merchant profile", body.message());
                })
                .verifyComplete();
    }

    @Test
    void recentAuditReturnsLogs() {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setAction("MERCHANT_ONBOARDED");

        when(auditService.recent(100)).thenReturn(Flux.just(log));

        StepVerifier.create(controller.recentAudit(100))
                .assertNext(entry -> assertEquals("MERCHANT_ONBOARDED", entry.getAction()))
                .verifyComplete();
    }
}
