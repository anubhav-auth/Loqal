package com.loqal.adminservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@SecurityRequirement(name = "bearerAuth")
public class SystemController {
    @Operation(summary = "Get system health", description = "Returns the health status of the platform")
    @ApiResponse(responseCode = "200", description = "System health retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Unauthorized")
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('view_metrics')")
    public ResponseEntity<String> getSystemHealth() {
        return ResponseEntity.ok("System is healthy");
    }
}