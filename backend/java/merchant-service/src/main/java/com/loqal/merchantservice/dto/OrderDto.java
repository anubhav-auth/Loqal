package com.loqal.merchantservice.dto;

import java.util.List;

public record OrderDto(String id, String customerName, List<ProductDto> items, String status, double totalAmount) {}
