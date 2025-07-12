package com.Loqal.OrderService.dto;

import com.Loqal.OrderService.entity.Order;
import com.Loqal.OrderService.entity.OrderItem;
import com.Loqal.OrderService.entity.OrderStatusHistory;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private Order order;
    private List<OrderItem> items;
    private List<OrderStatusHistory> statusHistory;
}
