package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.OrderEvent;
import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.OrderStatusUpdate;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.Loqal.orderservice.dto.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Order createOrder(OrderRequest orderRequest, UUID userId) {
        Order order = new Order();
        // ... map orderRequest to order entity ...
        order.setCustomerId(userId);
        order.setCurrentStatus("PENDING");
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

        if (OrderStatus.ORDER_CONFIRMED.equals(statusUpdate.getCurrentStatus())) {
            order.setCurrentStatus("CONFIRMED");
        } else {
            order.setCurrentStatus("REJECTED");
            // Optionally, you can store the rejection reason in the order entity
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}