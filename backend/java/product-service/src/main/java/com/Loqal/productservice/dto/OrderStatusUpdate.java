package com.Loqal.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdate {
    private UUID orderId;
    private OrderStatus status;
    private String reason;

    public enum OrderStatus {
        CONFIRMED,
        REJECTED
    }
}