package com.loqal.adminservice.service;

import com.loqal.adminservice.entity.AdminUser;
import com.loqal.adminservice.entity.dto.*;
import com.loqal.adminservice.entity.dto.MerchantDTO;
import com.loqal.adminservice.repository.AdminUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminUserRepository adminUserRepository;
    private final WebClient webClient;
    private final MerchantClientService merchantClientService;

    @Value("${uri.service_uri}")
    private String userServiceUrl;

    @Transactional
    public void createAdmin(AdminUser adminUser) {
        if (adminUserRepository.existsByUserId(adminUser.getUserId())) {
            throw new RuntimeException("Admin user already exists for user ID: " + adminUser.getUserId());
        }

        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setPermissions(List.of(AdminPermission.MANAGE_USERS));
        adminUserRepository.save(adminUser);
    }

    @Transactional
    public void upgradeUserToAdmin(UUID id) {
        String userServiceUri = userServiceUrl + "/users/profile/" + id.toString();
        UserProfileDto userProfile = webClient.get()
                .uri(userServiceUri)
                .retrieve()
                .bodyToMono(UserProfileDto.class)
                .block();

        if (userProfile == null) {
            throw new RuntimeException("User not found for ID: " + id);
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUserId(userProfile.getUserId());
        adminUser.setTenantId(userProfile.getTenantId());
        adminUser.setPermissions(List.of(AdminPermission.MANAGE_USERS));
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUserRepository.save(adminUser);
    }

    @Transactional
    public void updateAdminPermission(UUID userId, List<AdminPermission> permissions) {
        AdminUser adminUser = adminUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Admin user not found for ID: " + userId));

        adminUser.setPermissions(permissions);
        adminUser.setUpdatedAt(LocalDateTime.now());
        adminUserRepository.save(adminUser);
    }
}
