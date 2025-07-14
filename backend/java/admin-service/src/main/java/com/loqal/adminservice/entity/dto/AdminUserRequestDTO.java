package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class AdminUserRequestDTO {
    private UUID userId;
    private UUID tenantId;
    private List<String> permissions;
}