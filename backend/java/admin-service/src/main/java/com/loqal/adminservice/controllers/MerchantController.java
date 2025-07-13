package com.loqal.adminservice.controllers;

import com.loqal.adminservice.entity.dto.MerchantRequestDTO;
import com.loqal.adminservice.entity.dto.MerchantResponseDTO;
import com.loqal.adminservice.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MerchantController {
    private final MerchantService merchantService;

    @Operation(summary = "Onboard a new merchant", description = "Onboards a new merchant to the platform")
    @ApiResponse(responseCode = "201", description = "Merchant onboarded successfully")
    @ApiResponse(responseCode = "403", description = "Unauthorized")
    @PostMapping
    @PreAuthorize("hasAuthority('manage_merchants')")
    public ResponseEntity<MerchantResponseDTO> onboardMerchant(@RequestBody MerchantRequestDTO request) {
        MerchantResponseDTO response = merchantService.onboardMerchant(request);
        return ResponseEntity.status(201).body(response);
    }
}
