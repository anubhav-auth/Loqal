package com.loqal.platform.repository;

import com.loqal.platform.entity.AuditLog;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AuditLogRepository extends R2dbcRepository<AuditLog, UUID> {

    Flux<AuditLog> findAllByOrderByCreatedAtDesc();
}
