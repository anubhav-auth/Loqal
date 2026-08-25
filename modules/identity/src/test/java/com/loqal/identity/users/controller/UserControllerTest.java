package com.loqal.identity.users.controller;

import com.loqal.identity.users.entity.Address;
import com.loqal.identity.users.entity.dto.UserInfoDto;
import com.loqal.identity.users.entity.dto.UserOauthRegisterDto;
import com.loqal.identity.users.entity.dto.UserProfileDto;
import com.loqal.identity.users.entity.dto.UserRegisterDto;
import com.loqal.identity.users.entity.dto.UserRoles;
import com.loqal.identity.users.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserController(userService);
    }

    private Address sampleAddress() {
        return Address.builder()
                .street("123 Main St").city("Bangalore").state("KA")
                .postalCode("560001").country("IN").build();
    }

    @Test
    void registerFromOAuth_success() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserOauthRegisterDto dto = new UserOauthRegisterDto(
                "oauth@test.com", "OAuth User", "1234567890", "pic.jpg", tenantId, sampleAddress());

        UserInfoDto result = UserInfoDto.builder()
                .userId(userId).roles(List.of(UserRoles.USER)).tenantId(tenantId).build();

        when(userService.registerOrUpdateFromOAuth(dto)).thenReturn(Mono.just(result));

        StepVerifier.create(controller.registerFromOAuth(dto))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals(result, resp.getBody());
                })
                .verifyComplete();
    }

    @Test
    void getProfile_found() {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = UserProfileDto.builder()
                .userId(userId).fullName("Test User").email("test@test.com")
                .phoneNumber("1234567890").profilePictureUrl("pic.jpg")
                .tenantId(UUID.randomUUID()).address(sampleAddress()).build();

        when(userService.getProfile(userId)).thenReturn(Mono.just(profile));

        StepVerifier.create(controller.getProfile(userId))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals(profile, resp.getBody());
                })
                .verifyComplete();
    }

    @Test
    void getProfile_notFound() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));

        StepVerifier.create(controller.getProfile(userId))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void register_success() {
        UUID userId = UUID.randomUUID();
        UserRegisterDto dto = new UserRegisterDto(
                "New User", "new@test.com", "1234567890", "pic.jpg", UUID.randomUUID(), sampleAddress());

        UserProfileDto profile = UserProfileDto.builder()
                .userId(userId).fullName("New User").email("new@test.com")
                .phoneNumber("1234567890").profilePictureUrl("pic.jpg")
                .tenantId(dto.getTenantId()).address(sampleAddress()).build();

        when(userService.register(dto)).thenReturn(Mono.just(profile));

        StepVerifier.create(controller.register(dto))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CREATED, resp.getStatusCode());
                    assertEquals(profile, resp.getBody());
                })
                .verifyComplete();
    }

    @Test
    void register_duplicateEmail() {
        UserRegisterDto dto = new UserRegisterDto(
                "Dup User", "dup@test.com", "1234567890", null, UUID.randomUUID(), sampleAddress());

        when(userService.register(dto))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists")));

        StepVerifier.create(controller.register(dto))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void updateProfile_success() {
        UUID userId = UUID.randomUUID();
        UserProfileDto dto = UserProfileDto.builder()
                .fullName("Updated").phoneNumber("0987654321")
                .profilePictureUrl("newpic.jpg").address(sampleAddress()).build();

        UserProfileDto updated = UserProfileDto.builder()
                .userId(userId).fullName("Updated").email("test@test.com")
                .phoneNumber("0987654321").profilePictureUrl("newpic.jpg")
                .tenantId(UUID.randomUUID()).address(sampleAddress()).build();

        when(userService.updateProfile(userId, dto)).thenReturn(Mono.just(updated));

        StepVerifier.create(controller.updateProfile(userId, dto))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals(updated, resp.getBody());
                })
                .verifyComplete();
    }

    @Test
    void upgradeToMerchant_success() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(userService.upgradeToMerchant(userId, tenantId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.upgradeToMerchant(userId, tenantId))
                .assertNext(resp -> assertEquals(HttpStatus.OK, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void upgradeToMerchant_userNotFound() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(userService.upgradeToMerchant(userId, tenantId))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));

        StepVerifier.create(controller.upgradeToMerchant(userId, tenantId))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void upgradeToMerchant_alreadyMerchant() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(userService.upgradeToMerchant(userId, tenantId))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a merchant")));

        StepVerifier.create(controller.upgradeToMerchant(userId, tenantId))
                .expectError(ResponseStatusException.class)
                .verify();
    }
}
