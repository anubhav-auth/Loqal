package com.loqal.adminservice.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MerchantResponseDTO {
    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private String name;
    private String description;
    private String address;
    private String phoneNumber;
    private String email;
    private String logoUrl;
    private String status;
    private LocalDateTime onboardedAt;
}