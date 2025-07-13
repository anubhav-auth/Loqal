package com.loqal.adminservice.controllers;

import com.loqal.adminservice.entity.dto.AdminUserRequestDTO;
import com.loqal.adminservice.entity.dto.AdminUserResponseDTO;
import com.loqal.adminservice.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin-users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @Operation(summary = "Create a new admin user", description = "Creates an admin user with specified permissions")
    @ApiResponse(responseCode = "201", description = "Admin user created successfully")
    @ApiResponse(responseCode = "403", description = "Unauthorized")
    @PostMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<AdminUserResponseDTO> createAdminUser(@RequestBody AdminUserRequestDTO request) {
        AdminUserResponseDTO response = adminUserService.createAdminUser(request);
        return ResponseEntity.status(201).body(response);
    }
}
