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
@Table("refunds")
public class Refund {
    @Id
    private UUID id;
    private UUID tenantId;
    private UUID paymentId;
    private String razorpayRefundId;
    /** Amount in minor units (paise). */
    private long amountMinor;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";
}
