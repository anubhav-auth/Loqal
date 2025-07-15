package com.loqal.adminservice.controllers;

import com.loqal.adminservice.entity.AdminUser;
import com.loqal.adminservice.entity.Merchant;
import com.loqal.adminservice.entity.dto.AdminPermission;
import com.loqal.adminservice.entity.dto.AdminUserResponseDTO;
import com.loqal.adminservice.security.HasAdminPermission;
import com.loqal.adminservice.service.AdminService;
import com.loqal.adminservice.service.MerchantClientService;
import com.loqal.adminservice.utils.JwtService;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final AdminService adminService;
    private final MerchantClientService merchantClientService;
    private final JwtService jwtService;

    @HasAdminPermission(AdminPermission.MANAGE_ADMINS)
    @PostMapping("/create")
    public ResponseEntity<AdminUser> createAdminUser(@RequestBody AdminUser request) {
        AdminUser response = adminService.createAdmin(request);
        return ResponseEntity.status(201).body(response);
    }

    @HasAdminPermission(AdminPermission.MANAGE_ADMINS)
    @PostMapping("/create-from-user")
    public  ResponseEntity<AdminUser> createAdminFromUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        AdminUser response = adminService.upgradeUserToAdmin(UUID.fromString(userId));

        return  ResponseEntity.status(201).body(response);
    }


    @HasAdminPermission(AdminPermission.MANAGE_ADMINS)
    @PostMapping("/update-permission")
    public ResponseEntity<AdminUser> updateAdminPermission(@AuthenticationPrincipal Jwt jwt, UUID userId, List<AdminPermission> permissions) {
        String auditID = jwt.getClaimAsString("user_id");
        AdminUser adminUser = adminService.updateAdminPermission(userId, permissions);

        return ResponseEntity.status(200).body(adminUser);
    }

    @PostMapping("/merchants/onboard")
    public  ResponseEntity<?> onboardMerchant(Merchant merchant) throws JOSEException {
        Merchant merchant1 = merchantClientService.onboardMerchant(merchant, jwtService.generateServiceToken());
        return ResponseEntity.ok().body(merchant1);
    }

    //TODO
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
