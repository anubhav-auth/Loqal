package com.Loqal.userservice.controller;


import com.Loqal.userservice.entity.dto.UserInfoDto;
import com.Loqal.userservice.entity.dto.UserOauthRegisterDto;
import com.Loqal.userservice.entity.dto.UserProfileDto;
import com.Loqal.userservice.entity.dto.UserRegisterDto;
import com.Loqal.userservice.services.UserService;
import com.Loqal.userservice.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class UserController {

    private final UserService userService;
    private final static String TRUSTED_ISSUER = "auth-service";
    private final JwtUtils jwtUtils;

    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    // Internal

    @Hidden
    @PostMapping("/internal/users/oauth-register")
    public ResponseEntity<UserInfoDto> registerFromOAuth(
            @RequestBody UserOauthRegisterDto dto,
            @RequestHeader("Authorization") String bearerToken
    ) {
        String token = bearerToken.replace("Bearer ", "");
        if (!jwtUtils.validateIssuer(token, TRUSTED_ISSUER)) {
            return ResponseEntity.status(403).build();
        }

        UserInfoDto userInfo = userService.registerOrUpdateFromOAuth(dto);
        return ResponseEntity.ok(userInfo);
    }


    // Public
    @GetMapping("/users/profile/{id}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getProfile(id));
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


