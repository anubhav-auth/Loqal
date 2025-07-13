package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogResponseDTO {
    private Long id;
    private Long adminUserId;
    private Long tenantId;
    private String action;
    private String targetEntityType;
    private Long targetEntityId;
    private String details;
    private LocalDateTime timestamp;
}
