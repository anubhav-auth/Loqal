package com.loqal.merchantservice.dto;

import java.util.List;
import java.util.UUID;

public record OrderDto(UUID id, String customerName, List<ProductDto> items, String status, double totalAmount) {}
