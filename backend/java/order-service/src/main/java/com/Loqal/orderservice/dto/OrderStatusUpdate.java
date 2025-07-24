package com.Loqal.orderservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderStatusUpdate
{

        private UUID deliveryAgentId;   // Assign or change delivery agent
        private String paymentStatus;   // Update payment status
        private String currentStatus;   // Update order status
    }
