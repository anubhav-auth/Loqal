package com.Loqal.productservice.entity;

import java.util.List;

public record ProductDTO(
        String name,
        String description,
        Category category,
        double price,
        int quantity,
        List<String> image_urls) {
}

