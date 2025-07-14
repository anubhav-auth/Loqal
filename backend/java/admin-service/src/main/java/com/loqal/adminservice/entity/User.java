package com.loqal.adminservice.entity;


import com.loqal.adminservice.entity.dto.UserRoles;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String profilePictureUrl;
    private List<UserRoles> roles;
    private Address address;
    private UUID tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
