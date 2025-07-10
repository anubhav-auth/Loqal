package com.loqal.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
