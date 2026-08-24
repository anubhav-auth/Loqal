package com.loqal.catalog.promotions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("coupon_redemptions")
public class CouponRedemption {
    @Id
    private UUID id;
    private UUID couponId;
    private UUID tenantId;
    private UUID userId;
    private UUID orderId;
    private long discountMinor;
    private LocalDateTime redeemedAt;
}
