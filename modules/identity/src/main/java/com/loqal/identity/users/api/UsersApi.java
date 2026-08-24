package com.loqal.identity.users.api;

import com.loqal.identity.users.entity.dto.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Published API of the identity/users package.
 * Other modules must depend only on this interface — never on internal
 * services, repositories or entities.
 */
public interface UsersApi {

    Mono<UserInfoDto> registerOrUpdateFromOAuth(UserOauthRegisterDto dto);

    Mono<UserProfileDto> getProfile(UUID userId);

    /** Roles + tenant lookup by email; empty if unknown email. */
    Mono<UserInfoDto> findAuthSnapshotByEmail(String email);

    Mono<UserProfileDto> register(UserRegisterDto dto);

    Mono<Void> upgradeToMerchant(UUID userId, UUID tenantId);
}
