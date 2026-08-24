package com.loqal.orders.fulfillment.repository;

import com.loqal.orders.fulfillment.entity.Delivery;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DeliveryRepository extends R2dbcRepository<Delivery, UUID> {

    Mono<Delivery> findByOrderId(UUID orderId);

    Flux<Delivery> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Flux<Delivery> findAllByAgentIdOrderByCreatedAtDesc(UUID agentId);
}
