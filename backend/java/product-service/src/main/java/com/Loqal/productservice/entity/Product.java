package com.Loqal.productservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Entity
@Table(name = "products")

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;//product name

    private String description;//details about the product

    @Embedded
    private Category category;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = StringListConverter.class)
    private List<String> image_urls;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime created_at;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updated_at;

    @Column(nullable = false)
    private UUID merchantId;

}
