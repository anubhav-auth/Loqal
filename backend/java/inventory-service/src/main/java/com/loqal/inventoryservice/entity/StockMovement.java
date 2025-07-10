package com.loqal.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;

    @Column(name = "type", nullable = false)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}

