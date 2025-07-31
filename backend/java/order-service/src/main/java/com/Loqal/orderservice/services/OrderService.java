package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.*;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.repository.OrderRepository;
import com.nimbusds.jose.util.Pair;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Order createOrder(OrderRequest orderRequest, UUID userId) {
        Order order = new Order();
        order.setCustomerId(userId);
        order.setCurrentStatus(OrderStatus.ORDER_PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        OrderEvent orderEvent = new OrderEvent(
                savedOrder.getId(),
                orderRequest.getItemsOrdered().stream()
                        .map(item -> new OrderEvent.OrderItem(item.getId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );

        kafkaTemplate.send("order-events", orderEvent);

        return savedOrder;
    }

    @KafkaListener(topics = "${spring.kafka.topic.order-status-updates}", groupId = "order-service-group")
    public void consumeOrderStatusUpdate(OrderStatusUpdate statusUpdate) {
        Order order = orderRepository.findById(statusUpdate.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setCurrentStatus(statusUpdate.getStatus());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public List<Order> getOrdersByUserId(UUID userId) {
        Optional<List<Order>> allByCustomerId = orderRepository.findAllByCustomerId(userId);

        return allByCustomerId.orElseThrow(() -> new RuntimeException("No orders found for user ID: " + userId));
    }

    public Object getOrderByIdAndUserId(UUID orderId, UUID userId) {
        Optional<List<Order>> order = orderRepository.findAllByCustomerIdAndId(userId, orderId);
        if (order.isPresent() && order.get().get(0).getCustomerId().equals(userId)) {
            return order.get();
        } else {
            return new RuntimeException("Order not found for user ID: " + userId + " and order ID: " + orderId);
        }
    }

    @Transactional
    public void updateOrder(OrderUpdate orderUpdate) {
        Order order = orderRepository.findById(orderUpdate.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderUpdate.getOrderId()));

        if (!order.getCustomerId().equals(orderUpdate.getCustomerId())) {
            throw new RuntimeException("Unauthorized action: User does not own this order.");
        }

        if (orderUpdate.getDeliveryAgentId() != null) {
            order.setDeliveryAgentId(orderUpdate.getDeliveryAgentId());
        }
        if (orderUpdate.getPaymentStatus() != null) {
            order.setPaymentStatus(orderUpdate.getPaymentStatus());
        }
        if (orderUpdate.getCurrentStatus() != null) {
            order.setCurrentStatus(orderUpdate.getCurrentStatus());
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (!order.getCustomerId().equals(userId)) {
            throw new RuntimeException("Unauthorized action: User does not own this order.");
        }

        Pair<UUID, List<ProductOrderRequest>> products = Pair.of(orderId, order.getItemsOrdered().stream().map(item -> new ProductOrderRequest(item.getId(), item.getQuantity())).collect(Collectors.toList()));
        try {
            kafkaTemplate.send("order-cancel", products);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send order cancellation event: " + e.getMessage());
        }
    }

    public List<Order> getOrdersByMerchantId(UUID merchantId) {
        Optional<List<Order>> allByCustomerId = orderRepository.findAllByMerchantId(merchantId);

        return allByCustomerId.orElseThrow(() -> new RuntimeException("No orders found for merchant ID: " + merchantId));
    }
}