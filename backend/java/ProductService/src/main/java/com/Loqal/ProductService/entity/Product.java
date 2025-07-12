package com.Loqal.ProductService.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Generated;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;
import java.util.UUID;
@Table(name="products" )
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false)
    private String name;//product name

    private String description;//details about the product

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private boolean is_available;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = StringListConverter.class)
    private List<String> image_urls;

    @Temporal(TemporalType.TIMESTAMP)
    private Date created_at;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updated_at;

    @Column(nullable = false)
    private UUID merchantId;//ID of the merchant who owns this product. Useful for filtering per merchant.
}
