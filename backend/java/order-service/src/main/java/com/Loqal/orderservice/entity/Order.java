package com.Loqal.orderservice.entity;

import com.Loqal.orderservice.dto.OrderStatus;
import jakarta.persistence.*; // Make sure to use jakarta.persistence
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity // FIX: Added @Entity annotation
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // This is now correct
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;
    @Column(nullable = false)
    private UUID merchantId;

    private UUID deliveryAgentId;

    // FIX: Changed to a One-to-Many relationship with the new OrderItem entity
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String paymentStatus;
    private UUID deliveryAddressId;

    @Enumerated(EnumType.STRING) // Best practice for storing enums
    private OrderStatus currentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}