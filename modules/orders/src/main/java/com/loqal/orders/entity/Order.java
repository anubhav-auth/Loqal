package com.loqal.orders.entity;

import com.loqal.orders.dto.OrderStatus;
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
    /** Amounts in minor units (paise) per PRD §9.2. */
    private long totalAmountMinor;
    private long discountAmountMinor;
    private long finalAmountMinor;
    private String paymentStatus;
    private UUID deliveryAddressId;
    private OrderStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    @Version
    private Long version;
}