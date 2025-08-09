package com.loqal.merchantservice.dto;

import java.util.UUID;

public record ProductDto(UUID id, String name, String description, double price, int currentStock) {}