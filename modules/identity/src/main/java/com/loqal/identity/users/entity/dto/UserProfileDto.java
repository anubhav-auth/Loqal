package com.loqal.identity.users.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.loqal.identity.users.entity.Address;
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

