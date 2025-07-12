package com.loqal.authservice.entity.dto;

import com.loqal.authservice.entity.Address;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserOauthRegisterDto(
    String email,
    String fullName,
    String phoneNumber,
    String profilePictureUrl,
    UUID tenantId,
    Address address
){}

