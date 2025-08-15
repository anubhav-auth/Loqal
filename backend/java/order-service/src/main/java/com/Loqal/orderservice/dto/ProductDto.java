package com.Loqal.orderservice.dto;

import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String description,
        Category category,
        int quantity,
        List<String> image_urls
) {}