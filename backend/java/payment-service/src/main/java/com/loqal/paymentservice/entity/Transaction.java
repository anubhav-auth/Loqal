package com.loqal.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue
    private UUID id;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private UUID orderId;
    private UUID userId;
    private String tenantId;
    private BigDecimal amount;
    private BigDecimal tipAmount;
    private String currency;
    private String paymentMethod;
    private String status; // pending, paid, failed
    private String type; // payment, tip, refund, wallet
    private LocalDateTime createdAt;

}
