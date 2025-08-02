package com.Loqal.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId; // Stores the ID of the product from the Product Service

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double priceAtPurchase; // The price of the item when the order was made

    // Establishes the link back to the Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}