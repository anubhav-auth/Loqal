package com.loqal.adminservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "action", nullable = false)
    private String action; // e.g., "CREATE_MERCHANT", "UPDATE_CONFIG"

    @Column(name = "target_entity_type")
    private String targetEntityType; // e.g., "MERCHANT", "SYSTEM_CONFIG"

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details; // JSONB for flexible details

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
