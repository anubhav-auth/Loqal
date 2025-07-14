package com.loqal.adminservice.entity.dto;

import com.loqal.adminservice.entity.Address;
import lombok.Data;


@Data
public class MerchantDTO {
    private BusinessType businessType;
    private String businessName;
    private String taxId;
    private String description;
    private Address address;
    private String phoneNumber;
    private String logoUrl;
    private String status;
}