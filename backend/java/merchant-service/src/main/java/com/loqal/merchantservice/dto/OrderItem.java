package com.loqal.merchantservice.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
public class OrderItem {
    private UUID id;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private double priceAtPurchase;
}
