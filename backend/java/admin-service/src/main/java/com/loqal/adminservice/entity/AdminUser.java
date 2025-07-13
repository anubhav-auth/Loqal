package com.loqal.adminservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "admin_users")
@Data
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Foreign key to User Service

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId; // For multitenancy

    @ElementCollection
    @CollectionTable(name = "admin_permissions", joinColumns = @JoinColumn(name = "admin_user_id"))
    @Column(name = "permission")
    private List<String> permissions; // JSONB or array of strings (e.g., ["manage_merchants", "view_metrics"])

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}