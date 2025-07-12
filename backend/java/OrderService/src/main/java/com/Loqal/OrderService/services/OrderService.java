package com.Loqal.OrderService.services;

import com.Loqal.OrderService.entity.Order;
import com.Loqal.OrderService.entity.OrderItem;
import com.Loqal.OrderService.entity.OrderStatusHistory;
import com.Loqal.OrderService.repository.OrderItemRepository;
import com.Loqal.OrderService.repository.OrderRepository;
import com.Loqal.OrderService.repository.OrderStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderStatusHistoryRepository statusHistoryRepository;

    public Order create(Order order, List<OrderItem> items, List<OrderStatusHistory> statusHistory) {
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        UUID orderId = UUID.fromString(savedOrder.getId().toString());

        for (OrderItem item : items) {
            item.setOrderId(orderId);
            orderItemRepository.save(item);
        }

        for (OrderStatusHistory status : statusHistory) {
            status.setOrderId(orderId);
            status.setTimestamp(LocalDateTime.now());
            statusHistoryRepository.save(status);
        }

        return savedOrder;
    }

    public Optional<Order> getById(UUID id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }
    public Order updateStatus(UUID orderId, String newStatus, String notes) {
        return orderRepository.findById(orderId).map(order -> {
            order.setCurrentStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());

            OrderStatusHistory statusHistory = new OrderStatusHistory();
            statusHistory.setOrderId(order.getId());
            statusHistory.setStatus(newStatus);
            statusHistory.setTimestamp(LocalDateTime.now());
            statusHistory.setNotes(notes);
            statusHistoryRepository.save(statusHistory);

            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Order not found"));
    }

}
