package com.Loqal.OrderService.services;

import com.Loqal.OrderService.entity.Order;
import com.Loqal.OrderService.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;


    public Order createOrder(Order order) {
        order.setStatus("CREATED");
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        return orderRepository.save(order);
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order updateOrderStatus(String orderId, String newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(newStatus);
            order.setUpdatedAt(new Date());
            Order updatedOrder = orderRepository.save(order);

            return updatedOrder;
        }
        return null;
    }

    public boolean cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && !"CANCELLED".equals(order.getStatus())) {
            order.setStatus("CANCELLED");
            order.setUpdatedAt(new Date());
            orderRepository.save(order);

            return true;
        }
        return false;
    }
}
