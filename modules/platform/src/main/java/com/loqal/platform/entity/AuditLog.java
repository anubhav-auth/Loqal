package com.loqal.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_logs")
public class AuditLog implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID actorUserId;
    private String actorEmail;
    private String action;
    private String resourceType;
    private String resourceId;
    private String details;
    private LocalDateTime createdAt;

    @Transient
    private boolean newRecord = false;

    public void markNew() {
        this.newRecord = true;
    }

    @Override
    public boolean isNew() {
        return newRecord;
    }
}
