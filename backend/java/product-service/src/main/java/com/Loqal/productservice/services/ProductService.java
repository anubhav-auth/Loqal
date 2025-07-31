package com.Loqal.productservice.services;

import com.Loqal.productservice.dto.OrderEvent;
import com.Loqal.productservice.dto.OrderStatusUpdate;
import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.order-status-updates}")
    private String orderStatusUpdatesTopic;

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-events}", groupId = "product-service-group")
    public void consumeOrderEvent(OrderEvent orderEvent) {
        log.info("Received order event for order ID: {}", orderEvent.getOrderId());
        try {
            for (OrderEvent.OrderItem item : orderEvent.getItems()) {
                Product product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with ID: " + item.getProductId()));

                log.info("Processing product: {}, requested quantity: {}, available stock: {}", product.getName(), item.getQuantity(), product.getQuantity());

                if (product.getQuantity() < item.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName() + ". Requested: " + item.getQuantity() + ", Available: " + product.getQuantity());
                }

                product.setQuantity(product.getQuantity() - item.getQuantity());
                productRepository.save(product);
                log.info("Decremented stock for product {}. New stock: {}", product.getName(), product.getQuantity());
            }
            sendStatusUpdate(orderEvent.getOrderId(), OrderStatusUpdate.OrderStatus.CONFIRMED, "Order processed successfully.");
        } catch (Exception e) {
            log.error("Failed to process order {}: {}", orderEvent.getOrderId(), e.getMessage());
            sendStatusUpdate(orderEvent.getOrderId(), OrderStatusUpdate.OrderStatus.REJECTED, e.getMessage());
        }
    }

    private void sendStatusUpdate(UUID orderId, OrderStatusUpdate.OrderStatus status, String reason) {
        OrderStatusUpdate statusUpdate = new OrderStatusUpdate(orderId, status, reason);
        kafkaTemplate.send(orderStatusUpdatesTopic, statusUpdate);
        log.info("Published order status update for order ID {}: {}", orderId, status);
    }
}