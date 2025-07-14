package com.loqal.adminservice.controllers;

import com.loqal.adminservice.entity.dto.AdminUserResponseDTO;
import com.loqal.adminservice.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final AdminService adminService;


    @PostMapping
    public ResponseEntity<AdminUserResponseDTO> createAdminUser(@RequestBody AdminUserRequestDTO request) {
        AdminUserResponseDTO response = adminService.createAdminUser(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ){
        // This method should return a paginated list of users
        // Implementation will depend on the specific requirements and data available
        return ResponseEntity.ok("List of users not implemented yet");
    }

    @PostMapping("/merchants/onboard")
    public  ResponseEntity<?> onboardMerchant(@RequestBody AdminUserRequestDTO request) {
        // This method should handle the onboarding of a merchant
        // Implementation will depend on the specific requirements and data available
        return ResponseEntity.ok("Merchant onboarding not implemented yet");
    }

    @PostMapping("/merchants/onboard")
    public  ResponseEntity<?> getMerchants(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        // This method should return a paginated list of merchants
        // Implementation will depend on the specific requirements and data available
        return ResponseEntity.ok("Merchants list not implemented yet");
    }

    @PostMapping("/platform-stats")
    public  ResponseEntity<?> getPlatformStats(){
        // This method should return platform statistics
        // Implementation will depend on the specific requirements and data available
        return ResponseEntity.ok("Platform statistics not implemented yet");
    }
}
