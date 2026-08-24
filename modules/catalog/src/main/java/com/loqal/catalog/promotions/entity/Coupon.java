package com.loqal.catalog.promotions.entity;

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
@Table("coupons")
public class Coupon implements Persistable<UUID> {

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";

    @Id
    private UUID id;
    private UUID tenantId;
    private String code;
    private String discountType;
    /** PERCENT: basis points (1000 = 10%); FIXED: minor units off the subtotal. */
    private long value;
    private long minOrderValueMinor;
    private Long maxDiscountMinor;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimitGlobal;
    private Integer usageLimitPerUser;
    private int timesUsed;
    private boolean active;
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
