package com.loqal.orders.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.util.UUID;

@Data
public class OrderItem {
    @Id
    private UUID id;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    /** Amount in minor units (paise) per PRD §9.2. */
    private long priceAtPurchaseMinor;
}
