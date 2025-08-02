package com.Loqal.orderservice.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@Data
public class Product {
    private UUID id;

    @NotBlank(message = "Product name cannot be blank")
    private String name;
    private String description;
    private Category category;
    private double price;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    private List<String> image_urls;
    private Date created_at;
    private Date updated_at;
    private UUID merchantId;
}