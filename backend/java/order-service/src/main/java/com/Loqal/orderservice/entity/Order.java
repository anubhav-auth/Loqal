package com.Loqal.orderservice.entity;

import com.Loqal.orderservice.dto.OrderStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table("orders")
@Data
public class Order {
    @Id
    private UUID id;
    private UUID customerId;
    private UUID merchantId;
    private UUID deliveryAgentId;
    @Transient
    private List<OrderItem> items;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String paymentStatus;
    private UUID deliveryAddressId;
    private OrderStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private Long version;
}