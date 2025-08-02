package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.*;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.exception.InvalidOrderStatusException;
import com.Loqal.orderservice.repository.OrderRepository;
import com.nimbusds.jose.util.Pair;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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
    private final WebClient productServiceWebClient; // Inject the specific WebClient bean

    @Transactional
    public Order createOrder(OrderRequest orderRequest, UUID userId) {
        List<ProductOrderRequest> itemsToReserve = orderRequest.getItemsOrdered().stream()
                .map(item -> new ProductOrderRequest(item.getId(), item.getQuantity()))
                .collect(Collectors.toList());

        // Step 1: Synchronously call ProductService to reserve stock using WebClient
        try {
            productServiceWebClient.post()
                    .uri("/internal/products/reservations")
                    .bodyValue(itemsToReserve)
                    .retrieve()
                    // Handle specific status codes if needed
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new WebClientResponseException(
                                            response.statusCode().value(),
                                            "Client Error: " + errorBody,
                                            response.headers().asHttpHeaders(),
                                            errorBody.getBytes(),
                                            null
                                    ))
                            )
                    )
                    .toBodilessEntity() // We don't need the response body, just the status
                    .block(); // <-- This makes the reactive call synchronous. It waits for the result.

        } catch (WebClientResponseException e) {
            // The ProductService returned an error (e.g., 422 for insufficient stock).
            throw new RuntimeException("Failed to reserve stock: " + e.getResponseBodyAsString());
        }

        // Step 2: Stock was reserved successfully. Now, create the confirmed order.
        Order order = new Order();
        order.setCustomerId(userId);
        order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        // ... set items, total price, etc. ...

        Order savedOrder = orderRepository.save(order);

        // Step 3 (Optional): Publish an event for other downstream services.
        // kafkaTemplate.send("order-confirmed-events", ...);

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
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getCustomerId().equals(userId)) {
            throw new RuntimeException("Unauthorized action.");
        }
        if (order.getCurrentStatus() != OrderStatus.ORDER_CONFIRMED) {
            throw new RuntimeException("Order cannot be cancelled in its current state.");
        }

        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<ProductOrderRequest> itemsToRevert = order.getItemsOrdered().stream()
                .map(item -> new ProductOrderRequest(item.getId(), item.getQuantity()))
                .collect(Collectors.toList());

        // Use Kafka to asynchronously notify ProductService to revert stock
        kafkaTemplate.send("order-cancellation-events", itemsToRevert);
    }

    public List<Order> getOrdersByMerchantId(UUID merchantId) {
        Optional<List<Order>> allByCustomerId = orderRepository.findAllByMerchantId(merchantId);

        return allByCustomerId.orElseThrow(() -> new RuntimeException("No orders found for merchant ID: " + merchantId));
    }
}