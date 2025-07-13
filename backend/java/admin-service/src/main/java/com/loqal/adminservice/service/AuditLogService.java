package com.loqal.adminservice.service;

import com.loqal.adminservice.entity.AuditLog;
import com.loqal.adminservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void logAction(Long adminUserId, Long tenantId, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdminUserId(adminUserId);
        auditLog.setTenantId(tenantId);
        auditLog.setAction(action);
        auditLog.setTargetEntityType(entityType);
        auditLog.setTargetEntityId(entityId);
        auditLog.setDetails(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}