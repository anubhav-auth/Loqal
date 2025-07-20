package com.Loqal.productservice.entity;

import lombok.Data;

import java.util.UUID;
@Data
public class ProductOrderRequest {

    private UUID productId;
    private int quantity;


}

