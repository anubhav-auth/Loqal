package com.loqal.platform.repository;

import com.loqal.platform.entity.MerchantProfile;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MerchantProfileRepository extends R2dbcRepository<MerchantProfile, UUID> {

    Mono<MerchantProfile> findByTenantId(UUID tenantId);
}
