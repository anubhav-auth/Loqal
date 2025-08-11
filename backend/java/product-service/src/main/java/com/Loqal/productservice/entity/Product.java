package com.Loqal.productservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Table(name = "products")
public class Product {
    @Id
    private UUID id;

    private String name;

    private String description;

    private String category_name;

    private String category_description;

    private double price;

    private int quantity;

    private List<String> image_urls;

    private LocalDateTime created_at;

    private LocalDateTime updated_at;

    private UUID merchantId;
}
