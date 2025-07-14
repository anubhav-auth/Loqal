package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditLogResponseDTO {
    private UUID id;
    private UUID adminUserId;
    private UUID tenantId;
    private String action;
    private String targetEntityType;
    private UUID targetEntityId;
    private String details;
    private LocalDateTime timestamp;
}
