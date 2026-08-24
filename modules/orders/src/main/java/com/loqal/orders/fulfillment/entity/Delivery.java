package com.loqal.orders.fulfillment.entity;

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
@Table("deliveries")
public class Delivery implements Persistable<UUID> {

    public static final String ASSIGNED = "ASSIGNED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";

    @Id
    private UUID id;
    private UUID tenantId;
    private UUID orderId;
    private UUID agentId;
    private String status;
    private String pickupOtp;
    private String deliveredOtp;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private String failureReason;
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
