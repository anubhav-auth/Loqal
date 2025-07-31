package com.Loqal.orderservice.dto;

import lombok.Data;

import java.util.UUID;


@Data
public class OrderStatusUpdate {

    public OrderStatusUpdate(UUID orderId, OrderStatus status, String reason) {
    }
    private UUID orderId;          // Unique identifier for the order
    private OrderStatus status;    // New status of the order
    private String reason;
}