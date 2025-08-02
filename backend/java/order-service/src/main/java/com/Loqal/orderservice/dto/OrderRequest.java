package com.Loqal.orderservice.dto;

import com.Loqal.orderservice.entity.Product;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderRequest {
    private UUID customerId;
    private UUID merchantId;
    private List<Product> items;    // Use 'itemsOrdered' as per your mapping preference
    private Double totalAmount;
    private Double discountAmount;
    private Double finalAmount;
    private String paymentStatus;
    private UUID deliveryAddressId;
    private String currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}