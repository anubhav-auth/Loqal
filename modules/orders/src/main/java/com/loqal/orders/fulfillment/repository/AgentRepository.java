package com.loqal.orders.fulfillment.repository;

import com.loqal.orders.fulfillment.entity.Agent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AgentRepository extends R2dbcRepository<Agent, UUID> {

    Flux<Agent> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Mono<Agent> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    Flux<Agent> findByUserId(UUID userId);
}
