package com.Loqal.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;


import java.util.Date;
import java.util.List;
import java.util.UUID;


@Data
public class Product {
    private UUID id;
    private String name;
    private String description;
    private Category category;
    private double price;
    private long quantity;
    private List<String> image_urls;
    private Date created_at;
    private Date updated_at;
    private UUID merchantId;

}