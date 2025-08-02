package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.OrderStatus;
import com.Loqal.orderservice.dto.ProductOrderRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient productServiceWebClient;
    private final CompensationService compensationService;

    @Value("${spring.kafka.topic.order-cancel}") // Standardized topic name
    private String orderCancellationTopic;

    @Transactional
    public Order createOrder(OrderRequest orderRequest, UUID userId) {
        List<ProductOrderRequest> itemsToReserve = orderRequest.getItems().stream()
                .map(item -> new ProductOrderRequest(item.getId(), item.getQuantity()))
                .collect(Collectors.toList());

        // Step 1: Synchronously call ProductService to reserve stock
        try {
            productServiceWebClient.post()
                    .uri("/internal/reservations")
                    .bodyValue(itemsToReserve)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new RuntimeException("Product service failed: " + errorBody))
                            )
                    )
                    .toBodilessEntity()
                    .block(); // Make the call synchronous

        } catch (Exception e) {
            log.error("Failed to reserve stock during order creation. Aborting.", e);
            throw new RuntimeException("Failed to reserve stock: " + e.getMessage());
        }

        // Step 2: Stock was reserved successfully. Now, create the confirmed order.
        Order order = new Order();
        order.setCustomerId(userId);
        order.setMerchantId(orderRequest.getMerchantId());
        order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);
        order.setItems(orderRequest.getItems());
        order.setTotalAmount(orderRequest.getTotalAmount());
        order.setDiscountAmount(orderRequest.getDiscountAmount());
        order.setFinalAmount(orderRequest.getFinalAmount());
        order.setPaymentStatus(orderRequest.getPaymentStatus());
        order.setDeliveryAddressId(orderRequest.getDeliveryAddressId());
        order.setCreatedAt(LocalDateTime.now());

        // ADDED: Saga Compensation Logic
        try {
            Order savedOrder = orderRepository.save(order);
            log.info("Order {} created successfully for user {}", savedOrder.getId(), userId);
            // Optionally publish an "order-confirmed" event here
            return savedOrder;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to save order {} after reserving stock. Triggering compensation.", order.getId(), e);
            compensationService.scheduleStockReversion(itemsToReserve);
            throw new RuntimeException("Could not create order. Stock reservation has been reverted.");
        }
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getCustomerId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not own this order.");
        }
        if (order.getCurrentStatus() != OrderStatus.ORDER_CONFIRMED) {
            throw new IllegalStateException("Order cannot be cancelled in its current state.");
        }

        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Map order items to the request DTO for the product service
        List<ProductOrderRequest> itemsToRevert = order.getItems().stream()
                .map(item -> new ProductOrderRequest(item.getId(), item.getQuantity()))
                .collect(Collectors.toList());

        // FIXED: Use the correct, standardized Kafka topic name
        kafkaTemplate.send(orderCancellationTopic, itemsToRevert);
        log.info("Published cancellation event for order {}", orderId);
    }

    // Other methods...
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findAllByCustomerId(userId)
                .orElseThrow(() -> new RuntimeException("No orders found for user ID: " + userId));
    }

    public Order getOrderByIdAndUserId(UUID orderId, UUID userId) {
        return orderRepository.findAllByCustomerIdAndId(userId, orderId)
                .orElseThrow(() -> new RuntimeException("Order not found or you do not have permission to view it."));
    }

    public List<Order> getOrdersByMerchantId(UUID merchantId) {
        return orderRepository.findAllByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("No orders found for merchant ID: " + merchantId));
    }
}