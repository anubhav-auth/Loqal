package com.Loqal.productservice.dto;

import java.util.UUID;

public class OrderStatusUpdate {

    private UUID orderId;          // Unique identifier for the order
    private OrderStatus status;    // New status of the order
    private String reason;
    public OrderStatusUpdate(UUID orderId, OrderStatus status, String reason) {
    }
}