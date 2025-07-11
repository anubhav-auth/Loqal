package com.Loqal.OrderService.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "orders")
@Data
public class Order {
    @Id
    private String id;
    private String customerId;
    private List<String> productIds;
    private String status; // CREATED, SHIPPED, DELIVERED, CANCELLED
    private Date createdAt;
    private Date updatedAt;
}
