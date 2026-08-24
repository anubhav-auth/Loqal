package com.loqal.orders.dto;

import java.util.List;
import java.util.UUID;

public record OrderDto(UUID id, UUID customerId, List<?> items, OrderStatus status, double totalAmount) {}
