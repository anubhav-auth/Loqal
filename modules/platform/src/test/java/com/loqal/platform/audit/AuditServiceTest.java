package com.loqal.platform.audit;

import com.loqal.platform.entity.AuditLog;
import com.loqal.platform.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditService auditService;

    private final UUID actorUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditService = new AuditService(auditLogRepository);
        doReturn(Mono.empty()).when(auditLogRepository).save(any());
    }

    @Test
    void recordSavesAuditLogWithCorrectFields() {
        StepVerifier.create(auditService.record(
                        actorUserId, "actor@test.com", "MERCHANT_ONBOARDED",
                        "merchant_profile", "profile-id", "details here"))
                .verifyComplete();

        verify(auditLogRepository).save(argThat(log -> {
            AuditLog entry = (AuditLog) log;
            return entry.getActorUserId().equals(actorUserId)
                    && "actor@test.com".equals(entry.getActorEmail())
                    && "MERCHANT_ONBOARDED".equals(entry.getAction())
                    && "merchant_profile".equals(entry.getResourceType())
                    && "profile-id".equals(entry.getResourceId())
                    && "details here".equals(entry.getDetails())
                    && entry.isNew();
        }));
    }

    @Test
    void recordSwallowsErrorOnFailure() {
        doReturn(Mono.error(new RuntimeException("DB down")))
                .when(auditLogRepository).save(any());

        StepVerifier.create(auditService.record(
                        actorUserId, "actor@test.com", "ACTION",
                        "resource", "id", "details"))
                .verifyComplete();
    }

    @Test
    void recentReturnsLimitedLogs() {
        AuditLog log1 = new AuditLog();
        log1.setId(UUID.randomUUID());
        AuditLog log2 = new AuditLog();
        log2.setId(UUID.randomUUID());

        when(auditLogRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Flux.just(log1, log2));

        StepVerifier.create(auditService.recent(2))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void recentCapsAt500() {
        when(auditLogRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Flux.range(1, 600).map(i -> {
                    AuditLog log = new AuditLog();
                    log.setId(UUID.randomUUID());
                    return log;
                }));

        StepVerifier.create(auditService.recent(999))
                .expectNextCount(500)
                .verifyComplete();
    }
}
