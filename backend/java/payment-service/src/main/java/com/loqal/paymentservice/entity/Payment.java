package com.loqal.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private UUID id;
    private UUID tenantId;
    private UUID orderId;
    private UUID userId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private double amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters
}