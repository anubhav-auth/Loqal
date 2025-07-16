package com.loqal.paymentservice.entity.dto;

import lombok.Data;

@Data
public class RefundRequest {
    private String paymentId;
    private double amount;

    // Getters and setters
}