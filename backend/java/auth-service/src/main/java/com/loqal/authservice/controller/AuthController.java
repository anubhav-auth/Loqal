package com.loqal.authservice.controller;

import com.loqal.authservice.entity.dto.LoginRequest;
import com.loqal.authservice.entity.dto.RegisterRequest;
import com.loqal.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Authentication management operations")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "User registration", description = "Registers a new user and stores their credentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request or email already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Refresh JWT token", description = "Generates a new JWT token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token successfully refreshed"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        return authService.refreshToken(authHeader);
    }

    @Operation(summary = "Validate JWT token", description = "Validates a JWT token and checks if it is still valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "400", description = "Invalid token format"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        return authService.validateToken(authHeader);
    }

    @Operation(summary = "Initiate Google OAuth login", description = "Generates a redirect URL for Google OAuth authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redirect URL generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid provider")
    })
    @GetMapping("/login")
    public ResponseEntity<?> initiateGoogleLogin() {
        return authService.initiateOAuthLogin("google");
    }

    @Operation(summary = "Handle Google OAuth callback", description = "Processes Google OAuth callback and generates JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated via Google OAuth"),
            @ApiResponse(responseCode = "400", description = "Invalid code or state"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/google")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam("code") String code,
                                                  @RequestParam(value = "state", required = false) String state) {
        return authService.handleOAuthCallback("google", code, state);
    }
    @Operation(summary = "Handle Google OAuth for mobile", description = "Authenticates a mobile user via Google ID token and returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated via Google OAuth"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid ID token"),
            @ApiResponse(responseCode = "401", description = "OAuth authentication failed")
    })
    @PostMapping("/oauth/mobile/google")
    public ResponseEntity<?> handleMobileOAuth(@Valid @RequestBody Map<String, String> body) {
        return authService.handleMobileOAuth(body);
    }
}