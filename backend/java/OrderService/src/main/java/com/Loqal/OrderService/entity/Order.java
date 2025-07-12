package com.Loqal.OrderService.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Table(name="orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private UUID customerId;
    private UUID merchantId;
    private UUID deliveryagentId;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String paymentStatus;
    private Long deliveryAddressId;
    private String currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
