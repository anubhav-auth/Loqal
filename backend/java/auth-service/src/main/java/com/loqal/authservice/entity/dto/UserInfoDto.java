package com.loqal.authservice.entity.dto;

import java.util.List;
import java.util.UUID;

public record UserInfoDto(
        UUID userId,
        List<String> roles,
        UUID tenantId
) {
}

