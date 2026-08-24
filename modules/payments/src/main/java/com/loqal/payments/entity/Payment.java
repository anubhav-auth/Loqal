package com.loqal.payments.entity;

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
@Table("payments")
public class Payment {
    @Id
    private UUID id;
    private UUID tenantId;
    private UUID orderId;
    private UUID userId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    /** Amount in minor units (paise). */
    private long amountMinor;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_CAPTURED = "CAPTURED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_REFUNDED = "REFUNDED";
}
