package com.loqal.platform.controller;

import com.loqal.platform.entity.MerchantProfile;
import com.loqal.platform.onboarding.MerchantOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorefrontProfileControllerTest {

    private MerchantOnboardingService onboardingService;
    private StorefrontProfileController controller;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        onboardingService = mock(MerchantOnboardingService.class);
        controller = new StorefrontProfileController(onboardingService);
        jwt = Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("user_id", userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("sub", "user@test.com")
                .build();
    }

    @Test
    void getOwnReturnsProfileWhenFound() {
        MerchantProfile profile = new MerchantProfile();
        profile.setId(UUID.randomUUID());
        profile.setTenantId(tenantId);
        profile.setStoreName("My Store");

        when(onboardingService.getOrCreateOwnProfile(tenantId, userId, "user@test.com"))
                .thenReturn(Mono.just(profile));

        StepVerifier.create(controller.getOwn(jwt))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals("My Store", response.getBody().getStoreName());
                })
                .verifyComplete();
    }

    @Test
    void getOwnReturns404WhenNotFound() {
        when(onboardingService.getOrCreateOwnProfile(tenantId, userId, "user@test.com"))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.getOwn(jwt))
                .assertNext(response -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void updateOwnReturns200WithUpdatedProfile() {
        MerchantProfile updates = new MerchantProfile();
        updates.setStoreName("Updated Store");

        MerchantProfile saved = new MerchantProfile();
        saved.setId(UUID.randomUUID());
        saved.setTenantId(tenantId);
        saved.setStoreName("Updated Store");

        when(onboardingService.updateOwnProfile(eq(tenantId), any(MerchantProfile.class)))
                .thenReturn(Mono.just(saved));

        StepVerifier.create(controller.updateOwn(updates, jwt))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals("Updated Store", response.getBody().getStoreName());
                })
                .verifyComplete();
    }

    @Test
    void updateOwnReturns404WhenProfileNotFound() {
        MerchantProfile updates = new MerchantProfile();
        updates.setStoreName("Ghost");

        when(onboardingService.updateOwnProfile(eq(tenantId), any(MerchantProfile.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Merchant profile not found")));

        StepVerifier.create(controller.updateOwn(updates, jwt))
                .assertNext(response -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()))
                .verifyComplete();
    }
}
