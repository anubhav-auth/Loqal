package com.loqal.orders.repository;

import com.loqal.orders.entity.OutboxEvent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutboxEventRepository extends R2dbcRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findTop100ByOrderByCreatedAt();
}
