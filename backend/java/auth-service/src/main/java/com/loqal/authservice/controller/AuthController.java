package com.loqal.authservice.controller;

import com.loqal.authservice.entity.dto.AuthResponse;
import com.loqal.authservice.entity.dto.LoginRequest;
import com.loqal.authservice.entity.dto.RegisterRequest;
import com.loqal.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Tag(name = "Authentication", description = "Authentication management operations")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User login")
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "User registration")
    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, String>>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"))));
    }

    @Operation(summary = "Refresh JWT token")
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        return authService.refreshToken(token)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Handle Google OAuth callback")
    @GetMapping("/google/callback")
    public Mono<ResponseEntity<AuthResponse>> handleGoogleCallback(@RequestParam("code") String code) {
        return authService.handleOAuthCallback(code)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Handle Google OAuth for mobile")
    @PostMapping("/oauth/mobile/google")
    public Mono<ResponseEntity<AuthResponse>> handleMobileOAuth(@Valid @RequestBody Map<String, String> body) {
        return authService.handleMobileOAuth(body)
                .map(ResponseEntity::ok);
    }
}