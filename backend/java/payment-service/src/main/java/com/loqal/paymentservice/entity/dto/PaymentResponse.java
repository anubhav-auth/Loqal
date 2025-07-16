package com.loqal.paymentservice.entity.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentId;
    private String orderId;
    private double amount;
    private String status;
    private String paymentLink;
}

