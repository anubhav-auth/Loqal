package com.Loqal.productservice.dto;

import java.util.UUID;

public class OrderStatusUpdate{

    public OrderStatusUpdate(UUID orderId, OrderStatus status, String reason) {
    }
    private UUID orderId;          // Unique identifier for the order
    private OrderStatus status;    // New status of the order
    private String reason;
}