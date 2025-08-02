package com.Loqal.orderservice.dto.events;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Payload for the event sent FROM ProductService BACK TO OrderService
@Data
@NoArgsConstructor
public class StockReservationResponse {
    private UUID orderId;
    private String status; // e.g., "SUCCESS" or "FAILED"
    private String reason; // Optional failure reason

    public StockReservationResponse(UUID orderId) {
        this.orderId = orderId;
    }
}