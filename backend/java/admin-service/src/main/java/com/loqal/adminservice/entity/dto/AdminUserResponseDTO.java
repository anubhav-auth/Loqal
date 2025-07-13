package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AdminUserResponseDTO {
    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
