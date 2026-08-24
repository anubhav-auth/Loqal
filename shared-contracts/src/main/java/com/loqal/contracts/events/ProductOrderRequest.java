package com.loqal.contracts.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProductOrderRequest {
    private UUID productId;
    private int quantity;
}
