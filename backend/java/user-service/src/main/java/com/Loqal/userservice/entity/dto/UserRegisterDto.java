package com.Loqal.userservice.entity.dto;

import lombok.*;
import com.loqal.userservice.entity.Address;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class UserRegisterDto {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private UUID tenantId;
    private Address address;
}
