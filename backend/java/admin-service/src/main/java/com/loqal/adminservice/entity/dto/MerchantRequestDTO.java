package com.loqal.adminservice.entity.dto;

import lombok.Data;

@Data
public class MerchantRequestDTO {
    private Long userId;
    private Long tenantId;
    private String name;
    private String description;
    private String address;
    private String phoneNumber;
    private String email;
    private String logoUrl;
    private String status; // e.g., "ACTIVE", "INACTIVE", "PENDING_APPROVAL"
}
