package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdminUserRequestDTO {
    private Long userId;
    private Long tenantId;
    private List<String> permissions;
}