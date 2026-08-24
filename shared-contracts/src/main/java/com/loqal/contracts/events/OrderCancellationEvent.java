package com.loqal.contracts.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancellationEvent {
    private UUID orderId;
    private List<ProductOrderRequest> items;
}
