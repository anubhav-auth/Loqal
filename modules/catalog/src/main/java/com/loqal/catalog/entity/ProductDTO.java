package com.loqal.catalog.entity;

import java.util.List;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        String description,
        Category category,
        double price,
        int quantity,
        List<String> image_urls
) {}

