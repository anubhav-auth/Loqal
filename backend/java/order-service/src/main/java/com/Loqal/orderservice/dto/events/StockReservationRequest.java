package com.Loqal.orderservice.dto.events;

import com.Loqal.orderservice.dto.ProductOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

// Payload for the event sent FROM OrderService TO ProductService
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockReservationRequest {
    private UUID orderId;
    private List<ProductOrderRequest> items;
}