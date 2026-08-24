package com.loqal.orders.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderRequest {
    private UUID merchantId;

    /** Optional; discount is always recomputed server-side (PRD §8.1). */
    private String couponCode;
    private List<ProductOrderRequest> items;
    private Double totalAmount;
    private Double discountAmount;
    private Double finalAmount;
    private String paymentStatus;
    private UUID deliveryAddressId;
    private String currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}