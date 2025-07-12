package com.Loqal.userservice.entity.dto;

import com.loqal.userservice.entity.Address;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOauthRegisterDto {
    private String email;
    private String fullName;
    private String phoneNumber;
    private String profilePictureUrl;
    private UUID tenantId;
    private Address address;
}

