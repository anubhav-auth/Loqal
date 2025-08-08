package com.loqal.merchantservice.dto;

public record ProductDto(String id, String name, String description, double price, int currentStock) {}