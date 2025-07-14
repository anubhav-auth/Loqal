package com.loqal.adminservice.entity.dto;

import com.loqal.adminservice.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private Address address;
    private UUID tenantId;
}
