package com.Loqal.productservice.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponse {
    private UUID orderId;
    private String status; // "SUCCESS" or "FAILED"
    private String reason; // Optional: reason for failure
}