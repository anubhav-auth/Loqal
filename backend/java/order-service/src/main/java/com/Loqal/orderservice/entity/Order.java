package com.Loqal.orderservice.entity;

import com.Loqal.orderservice.dto.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name="orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;
    @Column(nullable = false)
    private UUID merchantId;

    private UUID deliveryAgentId;

    private List<Product> itemsOrdered;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String paymentStatus;
    private Long deliveryAddressId;
    private OrderStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}