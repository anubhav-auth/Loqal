package com.Loqal.orderservice.dto;

import com.Loqal.orderservice.entity.Product;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private List<Product> items;
}