package com.loqal.platform;

import com.loqal.identity.users.api.UsersApi;
import com.loqal.platform.audit.AuditService;
import com.loqal.platform.onboarding.MerchantOnboardingService;
import com.loqal.platform.repository.MerchantProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantOnboardingServiceTest {

    private UsersApi usersApi;
    private MerchantProfileRepository profileRepository;
    private MerchantOnboardingService service;

    private final UUID adminId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    private final com.loqal.identity.users.entity.dto.UserProfileDto profile =
            com.loqal.identity.users.entity.dto.UserProfileDto.builder()
                    .userId(userId).email("owner@store.com").tenantId(tenantId).build();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        usersApi = mock(UsersApi.class);
        profileRepository = mock(MerchantProfileRepository.class);
        AuditService auditService = mock(AuditService.class);
        when(auditService.record(any(), any(), Mockito.anyString(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(profileRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(usersApi.getProfile(userId)).thenReturn(Mono.just(profile));
        when(usersApi.upgradeToMerchant(eq(userId), eq(tenantId))).thenReturn(Mono.empty());
        service = new MerchantOnboardingService(usersApi, profileRepository, auditService);
    }

    private MerchantOnboardingService.OnboardRequest request(String storeName) {
        return new MerchantOnboardingService.OnboardRequest(
                userId, storeName, "desc", null, null);
    }

    @Test
    void onboardsMerchantAndCreatesProfile() {
        when(profileRepository.findByTenantId(tenantId)).thenReturn(Mono.empty());

        StepVerifier.create(service.onboard(request("Priya's Store"), adminId, "admin@loqal.dev"))
                .assertNext(saved -> {
                    org.junit.jupiter.api.Assertions.assertEquals(tenantId, saved.getTenantId());
                    org.junit.jupiter.api.Assertions.assertEquals("Priya's Store", saved.getStoreName());
                })
                .verifyComplete();
        Mockito.verify(usersApi).upgradeToMerchant(userId, tenantId);
    }

    @Test
    void rejectsUnknownUser() {
        when(usersApi.getProfile(userId)).thenReturn(Mono.empty());

        StepVerifier.create(service.onboard(request("Store"), adminId, "admin@loqal.dev"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void rejectsDuplicateProfileForTenant() {
        when(profileRepository.findByTenantId(tenantId))
                .thenReturn(Mono.just(new com.loqal.platform.entity.MerchantProfile()));

        StepVerifier.create(service.onboard(request("Store"), adminId, "admin@loqal.dev"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void rejectsBlankStoreName() {
        StepVerifier.create(service.onboard(request("  "), adminId, "admin@loqal.dev"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
