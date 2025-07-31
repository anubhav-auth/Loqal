package com.Loqal.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProductOrderRequest {
    private UUID productId;
    private int quantity;
}