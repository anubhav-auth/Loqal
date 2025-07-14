package com.loqal.adminservice.entity;

import com.loqal.adminservice.entity.dto.BusinessType;
import com.loqal.adminservice.entity.dto.Status;
import lombok.Data;

import java.util.UUID;

@Data
public class Merchant {
    private UUID userId;
    private UUID tenantId;
    private String name;
    private BusinessType businessType;
    private String businessName;
    private String taxId;
    private String description;
    private Address address;
    private String phoneNumber;
    private String email;
    private String logoUrl;
    private Status status;
}


