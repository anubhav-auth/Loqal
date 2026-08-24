package com.loqal.contracts.events;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class StockReservationResponse {
    private UUID orderId;
    private String status;
    private String reason;

    public StockReservationResponse(UUID orderId) {
        this.orderId = orderId;
    }

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
