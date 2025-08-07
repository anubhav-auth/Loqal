package com.Loqal.userservice.services;

import com.Loqal.userservice.entity.Address;
import com.Loqal.userservice.entity.User;
import com.Loqal.userservice.entity.dto.*;
import com.Loqal.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;

    public Mono<UserProfileDto> getProfile(UUID id) {
        return repo.findById(id)
                .map(this::mapToProfile)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for ID: " + id)));
    }

    @Transactional
    public Mono<UserProfileDto> register(UserRegisterDto dto) {

        String[] rolesAsStrings = List.of(UserRoles.USER)
                .stream()
                .map(Enum::name)
                .toArray(String[]::new);

        return repo.findByEmail(dto.getEmail())
                .flatMap(existingUser -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists")))
                .switchIfEmpty(Mono.defer(() -> repo.insertNewUser(
                        UUID.randomUUID(),
                        dto.getEmail(),
                        dto.getFullName(),
                        dto.getPhoneNumber(),
                        dto.getProfilePictureUrl(),
                        rolesAsStrings,
                        dto.getAddress().getStreet(),
                        dto.getAddress().getCity(),
                        dto.getAddress().getState(),
                        dto.getAddress().getPostalCode(),
                        dto.getAddress().getCountry(),
                        dto.getTenantId(),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ).map(this::mapToProfile)))
                .cast(UserProfileDto.class);
    }

    @Transactional
    public Mono<UserProfileDto> updateProfile(UUID id, UserProfileDto dto) {
        return repo.findById(id)
                .flatMap(user -> {
                    user.setFullName(dto.getFullName());
                    user.setPhoneNumber(dto.getPhoneNumber());
                    user.setProfilePictureUrl(dto.getProfilePictureUrl());
                    user.setStreet(dto.getAddress().getStreet());
                    user.setCity(dto.getAddress().getCity());
                    user.setState(dto.getAddress().getState());
                    user.setPostalCode(dto.getAddress().getPostalCode());
                    user.setCountry(dto.getAddress().getCountry());
                    user.setUpdatedAt(LocalDateTime.now());
                    return repo.save(user);
                })
                .map(this::mapToProfile)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    @Transactional
    public Mono<UserInfoDto> registerOrUpdateFromOAuth(UserOauthRegisterDto dto) {

        String[] rolesAsStrings = List.of(UserRoles.USER)
                .stream()
                .map(Enum::name)
                .toArray(String[]::new);

        return repo.findByEmail(dto.getEmail())
                .switchIfEmpty(Mono.defer(() -> repo.insertNewUser(
                        UUID.randomUUID(),
                        dto.getEmail(),
                        dto.getFullName(),
                        dto.getPhoneNumber(),
                        dto.getProfilePictureUrl(),
                        rolesAsStrings,
                        dto.getAddress().getStreet(),
                        dto.getAddress().getCity(),
                        dto.getAddress().getState(),
                        dto.getAddress().getPostalCode(),
                        dto.getAddress().getCountry(),
                        dto.getTenantId(),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )))
                .map(user -> UserInfoDto.builder()
                        .userId(user.getId())
                        .roles(user.getRoles())
                        .tenantId(user.getTenantId())
                        .build());
    }

    private UserProfileDto mapToProfile(User user) {
        Address address = Address.builder()
                .street(user.getStreet())
                .city(user.getCity())
                .state(user.getState())
                .postalCode(user.getPostalCode())
                .country(user.getCountry())
                .build();

        return UserProfileDto.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .tenantId(user.getTenantId())
                .address(address)
                .build();
    }
}