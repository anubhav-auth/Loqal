package com.Loqal.ProductService.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Generated;
import org.hibernate.annotations.GenericGenerator;


import java.util.Date;
import java.util.List;
import java.util.UUID;


@Data
@Entity
@Table(name="products" )

public class Product {
    @Id
    @GeneratedValue(generator = "uuid1")
    @GenericGenerator(name = "uuid1", strategy = "uuid1")
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;//product name

    private String description;//details about the product

    @Embedded
    private Category category;

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
    private UUID merchantId;
}
