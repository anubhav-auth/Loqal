package com.Loqal.userservice.controller;


import com.Loqal.userservice.entity.dto.UserInfoDto;
import com.Loqal.userservice.entity.dto.UserOauthRegisterDto;
import com.Loqal.userservice.entity.dto.UserProfileDto;
import com.Loqal.userservice.entity.dto.UserRegisterDto;
import com.Loqal.userservice.services.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final static String TRUSTED_ISSUER = "auth-service";

    // Internal
    @Hidden
    @PostMapping("/internal/users/oauth-register")
    public ResponseEntity<UserInfoDto> registerFromOAuth(
            @RequestBody UserOauthRegisterDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String issuer = String.valueOf(jwt.getIssuer());

        if (!TRUSTED_ISSUER.equals(issuer)) {
            return ResponseEntity.status(403).build();
        }

        UserInfoDto userInfo = userService.registerOrUpdateFromOAuth(dto);
        return ResponseEntity.ok(userInfo);
    }


    // Public
    @GetMapping("/users/profile/{id}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(userService.getProfile(id));
        } catch (Exception e) {
            throw new RuntimeException("User not found for ID: " + id, e);
        }

    }

    @PostMapping("/users/register")
    public ResponseEntity<UserProfileDto> register(@RequestBody UserRegisterDto dto) {
        return ResponseEntity.ok(userService.register(dto));
    }

    @PutMapping("/users/profile/{id}")
    public ResponseEntity<UserProfileDto> updateProfile(@PathVariable UUID id, @RequestBody UserProfileDto dto) {
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }
}


