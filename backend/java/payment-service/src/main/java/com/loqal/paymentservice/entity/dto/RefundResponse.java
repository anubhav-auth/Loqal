package com.loqal.paymentservice.entity.dto;

import lombok.Data;

@Data
public class RefundResponse {
    private String refundId;
    private String paymentId;
    private double amount;
    private String status;

    // Getters and setters
}