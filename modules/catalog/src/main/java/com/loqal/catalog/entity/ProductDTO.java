package com.loqal.catalog.entity;

import java.util.List;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        String description,
        Category category,
        long priceMinor,
        int quantity,
        List<String> image_urls
) {}

