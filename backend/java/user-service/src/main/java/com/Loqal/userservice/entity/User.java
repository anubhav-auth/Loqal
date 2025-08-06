package com.Loqal.userservice.entity;

import com.Loqal.userservice.entity.dto.UserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column("email")
    private String email;

    @Column("full_name")
    private String fullName;

    @Column("phone_number")
    private String phoneNumber;

    @Column("profile_picture_url")
    private String profilePictureUrl;


    @Column("roles")
    private List<UserRoles> roles;

    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}