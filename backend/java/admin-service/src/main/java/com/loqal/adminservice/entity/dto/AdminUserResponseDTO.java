package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserResponseDTO {
    private Long id;
    private Long userId;
    private Long tenantId;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
