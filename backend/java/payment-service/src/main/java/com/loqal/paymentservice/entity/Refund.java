package com.loqal.paymentservice.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "refunds")
public class Refund {
    @Id
    private UUID id;
    private UUID tenantId;
    private UUID paymentId;
    private String razorpayRefundId;
    private double amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters
}