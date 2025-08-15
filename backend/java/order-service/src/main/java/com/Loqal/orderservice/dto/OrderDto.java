package com.Loqal.orderservice.dto;

import java.util.List;
import java.util.UUID;

public record OrderDto(UUID id, UUID customerId, List<ProductDto> items, OrderStatus status, double totalAmount) {}
