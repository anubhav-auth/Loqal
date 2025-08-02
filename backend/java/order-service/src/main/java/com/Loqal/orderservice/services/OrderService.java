package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.OrderStatus;
import com.Loqal.orderservice.dto.ProductOrderRequest;
import com.Loqal.orderservice.dto.events.StockReservationResponse;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.entity.OrderItem;
import com.Loqal.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient productServiceWebClient;
    private final OutboxService outboxService;

    @Value("${spring.kafka.topic.order-cancel}") // Standardized topic name
    private String orderCancellationTopic;

    @Transactional
    public Order createOrder(OrderRequest orderRequest, UUID userId) {
        Order order = new Order();
        order.setCustomerId(userId);
        order.setMerchantId(orderRequest.getMerchantId());
        order.setCurrentStatus(OrderStatus.ORDER_PENDING);
        order.setTotalAmount(orderRequest.getTotalAmount());
        order.setDiscountAmount(orderRequest.getDiscountAmount());
        order.setFinalAmount(orderRequest.getFinalAmount());
        order.setPaymentStatus(orderRequest.getPaymentStatus());
        order.setDeliveryAddressId(orderRequest.getDeliveryAddressId());
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = orderRequest.getItems().stream()
                .map(productDto -> {
                    OrderItem item = new OrderItem();
                    item.setProductId(productDto.getProductId());
                    item.setQuantity(productDto.getQuantity());
                    item.setPriceAtPurchase(productDto.getPrice());
                    item.setOrder(order);
                    return item;
                })
                .collect(Collectors.toList());

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        outboxService.requestStockReservation(savedOrder);

        log.info("Order {} created in PENDING state for user {}. Awaiting stock confirmation.", savedOrder.getId(), userId);
        return savedOrder;
    }

    // NEW: Kafka Listener to handle the result from ProductService
    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.stock-reservation-result}", groupId = "order-service-group")
    public void consumeStockReservationResult(StockReservationResponse response) {
        log.info("Received stock reservation result for order {}: {}", response.getOrderId(), response.getStatus());

        Order order = orderRepository.findById(response.getOrderId())
                .orElseThrow(() -> new RuntimeException("Received stock result for non-existent order: " + response.getOrderId()));

        if (order.getCurrentStatus() != OrderStatus.ORDER_PENDING) {
            log.warn("Received stock result for order {} which is no longer in PENDING state. Current state: {}. Ignoring.", order.getId(), order.getCurrentStatus());
            return;
        }

        if ("SUCCESS".equals(response.getStatus())) {
            order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);
        } else {
            order.setCurrentStatus(OrderStatus.ORDER_REJECTED);
            // You could store the failure reason from response.getReason() in the order entity.
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getCustomerId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not own this order.");
        }
        if (order.getCurrentStatus() != OrderStatus.ORDER_CONFIRMED && order.getCurrentStatus() != OrderStatus.ORDER_PENDING) {
            throw new IllegalStateException("Order cannot be cancelled in its current state.");
        }


        if (order.getCurrentStatus() == OrderStatus.ORDER_CONFIRMED) {
            outboxService.requestStockReversion(order.getItems().stream()
                    .map(item -> new ProductOrderRequest(item.getProductId(), item.getPriceAtPurchase(), item.getQuantity()))
                    .collect(Collectors.toList()));

            log.info("Scheduled stock reversion for cancelled order {}", orderId);
        }
        order.setCurrentStatus(OrderStatus.ORDER_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    // Other methods...
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findAllByCustomerId(userId);
    }

    public Order getOrderByIdAndUserId(UUID orderId, UUID userId) {
        return orderRepository.findAllByCustomerIdAndId(userId, orderId)
                .orElseThrow(() -> new RuntimeException("Order not found or you do not have permission to view it."));
    }

    public List<Order> getOrdersByMerchantId(UUID merchantId) {
        return orderRepository.findAllByMerchantId(merchantId);
    }
}