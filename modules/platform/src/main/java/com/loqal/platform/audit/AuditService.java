package com.loqal.platform.audit;

import com.loqal.platform.entity.AuditLog;
import com.loqal.platform.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Append-only audit trail (PRD §8.3). Failures to audit MUST NOT break the
 * caller's flow — errors are swallowed after logging.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Mono<Void> record(UUID actorUserId, String actorEmail, String action,
                             String resourceType, String resourceId, String details) {
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID());
        entry.setActorUserId(actorUserId);
        entry.setActorEmail(actorEmail);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setDetails(details);
        entry.setCreatedAt(java.time.LocalDateTime.now());
        entry.markNew();
        return auditLogRepository.save(entry)
                .doOnError(e -> org.slf4j.LoggerFactory.getLogger(AuditService.class)
                        .error("AUDIT WRITE FAILED for action {}: {}", action, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    public Flux<AuditLog> recent(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().take(Math.min(limit, 500));
    }
}
