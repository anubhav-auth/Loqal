package com.Loqal.userservice.services;

import com.Loqal.userservice.entity.User;
import com.Loqal.userservice.entity.dto.UserInfoDto;
import com.Loqal.userservice.entity.dto.UserOauthRegisterDto;
import com.Loqal.userservice.entity.dto.UserProfileDto;
import com.Loqal.userservice.entity.dto.UserRegisterDto;
import com.Loqal.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class UserService {

    private final UserRepository repo;

    @Autowired
    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public UserInfoDto getUserInfoByEmail(String email) {
        User user = (User) repo.findByEmail(email).orElseThrow(() ->
                new RuntimeException("User not found for email: " + email));

        return UserInfoDto.builder()
                .userId(user.getId())
                .roles(user.getRoles())
                .tenantId(user.getTenantId())
                .build();
    }

    public UserProfileDto getProfile(UUID id) {
        User user = repo.findById(id).orElseThrow(() ->
                new RuntimeException("User not found for ID: " + id));
        return mapToProfile(user);
    }

    public UserProfileDto register(UserRegisterDto dto) {
        if (repo.findByEmail(dto.getEmail()).isPresent())
            throw new RuntimeException("Email already exists");

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .profilePictureUrl(dto.getProfilePictureUrl())
                .tenantId(dto.getTenantId())
                .address(dto.getAddress())
                .roles(List.of("USER"))
                .build();

        User saved = repo.save(user);
        return mapToProfile(saved);
    }

    public UserProfileDto updateProfile(UUID id, UserProfileDto dto) {
        User user = repo.findById(id).orElseThrow(() ->
                new RuntimeException("User not found"));

        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setProfilePictureUrl(dto.getProfilePictureUrl());
        user.setAddress(dto.getAddress());

        User updated = repo.save(user);
        return mapToProfile(updated);
    }

    private UserProfileDto mapToProfile(User user) {
        return UserProfileDto.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .tenantId(user.getTenantId())
                .address(user.getAddress())
                .build();
    }

    public UserInfoDto registerOrUpdateFromOAuth(UserOauthRegisterDto dto) {
        Optional<Object> optionalUser = repo.findByEmail(dto.getEmail());

        User user;
        if (optionalUser.isPresent()) {
            user = (User) optionalUser.get();
            user.setFullName(dto.getFullName());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setProfilePictureUrl(dto.getProfilePictureUrl());
            user.setAddress(dto.getAddress());
        } else {
            user = User.builder()
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .phoneNumber(dto.getPhoneNumber())
                    .profilePictureUrl(dto.getProfilePictureUrl())
                    .address(dto.getAddress())
                    .tenantId(dto.getTenantId())
                    .roles(List.of("USER"))
                    .build();
        }

        User saved = repo.save(user);

        return UserInfoDto.builder()
                .userId(saved.getId())
                .roles(saved.getRoles())
                .tenantId(saved.getTenantId())
                .build();
    }
}

