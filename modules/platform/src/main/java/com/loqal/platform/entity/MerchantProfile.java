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
@Table("merchant_profiles")
public class MerchantProfile implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private String storeName;
    private String description;
    private String logoUrl;
    private String supportPhone;
    private String addressLine;
    private String city;
    /** Payout routing placeholder (Phase 3). Serialized JSON. */
    private String payoutConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
