package com.loqal.catalog.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Table(name = "products")
public class Product implements Persistable<UUID> {
    @Id
    private UUID id;

    @Transient
    private boolean newRecord = false;

    @Override
    public boolean isNew() {
        return newRecord;
    }

    public void markNew() {
        this.newRecord = true;
    }

    private String name;

    private String description;

    private String category_name;

    private String category_description;

    /** Amount in minor units (paise) per PRD §9.2. */
    private long priceMinor;

    private int quantity;

    private List<String> image_urls;

    private LocalDateTime created_at;

    private LocalDateTime updated_at;

    private UUID merchantId;
}
