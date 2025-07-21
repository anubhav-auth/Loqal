package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.entity.OrderItem;
import com.Loqal.orderservice.entity.OrderStatusHistory;
import com.Loqal.orderservice.repository.OrderItemRepository;
import com.Loqal.orderservice.repository.OrderRepository;
import com.Loqal.orderservice.repository.OrderStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderStatusHistoryRepository statusHistoryRepository;

    public Order create(OrderRequest or, UUID userID) {
        Order o = new Order();

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

    public Order updateStatus(UUID orderId, OrderDTO orderDTO) {
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