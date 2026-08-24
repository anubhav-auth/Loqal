package com.loqal.identity.users.controller;

import com.loqal.identity.users.entity.dto.UserInfoDto;
import com.loqal.identity.users.entity.dto.UserOauthRegisterDto;
import com.loqal.identity.users.entity.dto.UserProfileDto;
import com.loqal.identity.users.entity.dto.UserRegisterDto;
import com.loqal.identity.users.services.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Internal
    @Hidden
    @PostMapping("/internal/users/oauth-register")
    public Mono<ResponseEntity<UserInfoDto>> registerFromOAuth(@RequestBody UserOauthRegisterDto dto) {
        return userService.registerOrUpdateFromOAuth(dto)
                .map(ResponseEntity::ok);
    }

    // Public
    @GetMapping("/users/profile/{id}")
    public Mono<ResponseEntity<UserProfileDto>> getProfile(@PathVariable UUID id) {
        return userService.getProfile(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/users/register")
    public Mono<ResponseEntity<UserProfileDto>> register(@RequestBody UserRegisterDto dto) {
        return userService.register(dto)
                .map(userProfile -> ResponseEntity.status(HttpStatus.CREATED).body(userProfile));
    }

    @PutMapping("/users/profile/{id}")
    public Mono<ResponseEntity<UserProfileDto>> updateProfile(@PathVariable UUID id, @RequestBody UserProfileDto dto) {
        return userService.updateProfile(id, dto)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/users/{id}/upgrade-merchant")
    public Mono<ResponseEntity<Void>> upgradeToMerchant(@PathVariable UUID id, @RequestParam UUID tenantId) {
        return userService.upgradeToMerchant(id, tenantId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + id)));
    }

}
