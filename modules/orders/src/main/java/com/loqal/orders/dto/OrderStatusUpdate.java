package com.loqal.orders.dto;

import lombok.Data;

import java.util.UUID;


@Data
public class OrderStatusUpdate {

    private UUID orderId;          // Unique identifier for the order
    private OrderStatus status;    // New status of the order
    private String reason;

    public OrderStatusUpdate(UUID orderId, OrderStatus status, String reason) {
    }
}