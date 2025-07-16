package com.loqal.paymentservice.entity.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String orderId;
    private double amount;
    private String currency;

    // Getters and setters
}
