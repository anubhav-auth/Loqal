package com.loqal.adminservice.repository;

import com.loqal.adminservice.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    @Query("SELECT au FROM AdminUser au WHERE au.userId = :userId AND au.tenantId = :tenantId")
    AdminUser findByUserIdAndTenantId(Long userId, Long tenantId);
}
