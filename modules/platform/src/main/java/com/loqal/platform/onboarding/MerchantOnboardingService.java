package com.loqal.platform.onboarding;

import com.loqal.identity.users.api.UsersApi;
import com.loqal.platform.audit.AuditService;
import com.loqal.platform.entity.MerchantProfile;
import com.loqal.platform.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Merchant onboarding (PRD §8.3): upgrades an existing user to MERCHANT and
 * creates the storefront profile bound to the user's personal tenant.
 */
@Service
public class MerchantOnboardingService {

    private final UsersApi usersApi;
    private final MerchantProfileRepository profileRepository;
    private final AuditService auditService;

    public MerchantOnboardingService(UsersApi usersApi,
                                     MerchantProfileRepository profileRepository,
                                     AuditService auditService) {
        this.usersApi = usersApi;
        this.profileRepository = profileRepository;
        this.auditService = auditService;
    }

    public record OnboardRequest(UUID userId, String storeName, String description,
                                 String logoUrl, String supportPhone) {}

    public Mono<MerchantProfile> onboard(OnboardRequest request, UUID adminUserId, String adminEmail) {
        if (request.storeName() == null || request.storeName().isBlank()) {
            return Mono.error(new IllegalArgumentException("storeName is required"));
        }
        return usersApi.getProfile(request.userId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(profile -> {
                    UUID tenantId = profile.getTenantId();
                    return upgradeAndCreateProfile(request, tenantId)
                            .flatMap(saved -> auditService.record(adminUserId, adminEmail,
                                    "MERCHANT_ONBOARDED", "merchant_profile",
                                    saved.getId().toString(),
                                    "user=" + request.userId() + " tenant=" + tenantId + " store=" + request.storeName())
                                    .thenReturn(saved));
                });
    }

    private Mono<MerchantProfile> upgradeAndCreateProfile(OnboardRequest request, UUID tenantId) {
        return usersApi.upgradeToMerchant(request.userId(), tenantId)
                .then(profileRepository.findByTenantId(tenantId))
                .flatMap(existing -> Mono.<MerchantProfile>error(new IllegalStateException(
                        "Tenant already has a merchant profile")))
                .switchIfEmpty(Mono.defer(() -> {
                    MerchantProfile profile = new MerchantProfile();
                    profile.setId(UUID.randomUUID());
                    profile.setTenantId(tenantId);
                    profile.setUserId(request.userId());
                    profile.setStoreName(request.storeName());
                    profile.setDescription(request.description());
                    profile.setLogoUrl(request.logoUrl());
                    profile.setSupportPhone(request.supportPhone());
                    profile.setCreatedAt(LocalDateTime.now());
                    profile.markNew();
                    return profileRepository.save(profile);
                }));
    }

    public Mono<MerchantProfile> getOrCreateOwnProfile(UUID tenantId, UUID userId, String fallbackStoreName) {
        return profileRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.defer(() -> {
                    MerchantProfile profile = new MerchantProfile();
                    profile.setId(UUID.randomUUID());
                    profile.setTenantId(tenantId);
                    profile.setUserId(userId);
                    profile.setStoreName(fallbackStoreName == null || fallbackStoreName.isBlank()
                            ? "My Store" : fallbackStoreName);
                    profile.setCreatedAt(LocalDateTime.now());
                    profile.markNew();
                    return profileRepository.save(profile);
                }));
    }

    public Mono<MerchantProfile> updateOwnProfile(UUID tenantId, MerchantProfile updates) {
        return profileRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant profile not found")))
                .flatMap(existing -> {
                    if (updates.getStoreName() != null) existing.setStoreName(updates.getStoreName());
                    if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
                    if (updates.getLogoUrl() != null) existing.setLogoUrl(updates.getLogoUrl());
                    if (updates.getSupportPhone() != null) existing.setSupportPhone(updates.getSupportPhone());
                    if (updates.getAddressLine() != null) existing.setAddressLine(updates.getAddressLine());
                    if (updates.getCity() != null) existing.setCity(updates.getCity());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return profileRepository.save(existing);
                });
    }
}
