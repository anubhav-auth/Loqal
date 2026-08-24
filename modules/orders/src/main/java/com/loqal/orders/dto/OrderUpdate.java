package com.loqal.orders.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderUpdate {
    private UUID orderId;          // Unique identifier for the order
    private UUID customerId;       // Customer who placed the order
    private UUID deliveryAgentId;   // Assign or change delivery agent
    private String paymentStatus;   // Update payment status
    private OrderStatus currentStatus;   // Update order status
}
