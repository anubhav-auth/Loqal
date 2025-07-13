package com.loqal.adminservice.service;

import com.loqal.adminservice.entity.AdminUser;
import com.loqal.adminservice.entity.dto.AdminUserRequestDTO;
import com.loqal.adminservice.entity.dto.AdminUserResponseDTO;
import com.loqal.adminservice.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;
    private final WebClient webClient; // For calling User Service

    public AdminUserResponseDTO createAdminUser(AdminUserRequestDTO request) {
        // Validate tenant and user via User Service
        String userServiceUrl = "http://user-service.default.svc.cluster.local/users/" + request.getUserId();
        webClient.get().uri(userServiceUrl).retrieve().bodyToMono(String.class).block(); // Simplified; add error handling

        AdminUser adminUser = new AdminUser();
        adminUser.setUserId(request.getUserId());
        adminUser.setTenantId(request.getTenantId());
        adminUser.setPermissions(request.getPermissions());
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = adminUserRepository.save(adminUser);

        return mapToResponseDTO(adminUser);
    }

    private AdminUserResponseDTO mapToResponseDTO(AdminUser adminUser) {
        AdminUserResponseDTO response = new AdminUserResponseDTO();
        response.setId(adminUser.getId());
        response.setUserId(adminUser.getUserId());
        response.setTenantId(adminUser.getTenantId());
        response.setPermissions(adminUser.getPermissions());
        response.setCreatedAt(adminUser.getCreatedAt());
        response.setUpdatedAt(adminUser.getUpdatedAt());
        return response;
    }
}
