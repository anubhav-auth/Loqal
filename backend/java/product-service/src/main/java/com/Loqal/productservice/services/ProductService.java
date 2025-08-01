package com.Loqal.productservice.services;

import com.Loqal.productservice.dto.OrderEvent;
import com.Loqal.productservice.dto.OrderStatus;
import com.Loqal.productservice.dto.OrderStatusUpdate;
import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.repository.ProductRepository;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
            sendStatusUpdate(orderEvent.getOrderId(), OrderStatus.ORDER_CONFIRMED, "Order processed successfully.");
        } catch (Exception e) {
            log.error("Failed to process order {}: {}", orderEvent.getOrderId(), e.getMessage());
            sendStatusUpdate(orderEvent.getOrderId(), OrderStatus.ORDER_REJECTED, e.getMessage());
        }
    }

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-cancel}", groupId = "product-service-group")
    public void consumeOrderCancellation(Pair<UUID, List<ProductOrderRequest>> products) {
        log.info("Received order cancel request");
        UUID orderId = products.getLeft();
        try {
            for (ProductOrderRequest item : products.getRight()) {

                Product product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with ID: " + item.getProductId()));

                log.info("Processing product: {}, requested quantity: {}, available stock: {}", product.getName(), item.getQuantity(), product.getQuantity());

                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
                log.info("Incremented stock for product {}. New stock: {}", product.getName(), product.getQuantity());
            }
            sendStatusUpdate(orderId, OrderStatus.ORDER_CANCELLED, "Order processed successfully.");
        } catch (Exception e) {
            log.error("Failed to cancel order {}: {}", orderId, e.getMessage());
            sendStatusUpdate(orderId, OrderStatus.ORDER_REJECTED, e.getMessage());
        }
    }

    private void sendStatusUpdate(UUID orderId, OrderStatus status, String reason) {
        OrderStatusUpdate statusUpdate = new OrderStatusUpdate(orderId, status, reason);
        kafkaTemplate.send(orderStatusUpdatesTopic, statusUpdate);
        log.info("Published order status update for order ID {}: {}", orderId, status);
    }

    public Object create(ProductDTO product, UUID merchantId) {
        if (product == null || product.name() == null || product.price() <= 0 || product.quantity() < 0) {
            throw new IllegalArgumentException("Invalid product data provided.");
        }
        Product newProduct = new Product();
        newProduct.setName(product.name());
        newProduct.setPrice(product.price());
        newProduct.setQuantity(product.quantity());
        newProduct.setMerchantId(merchantId);
        return productRepository.save(newProduct);
    }

    public Object getAll(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null.");
        }
        return productRepository.findAllByMerchantId(tenantId);
    }

    public Product getById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

    public Product update(UUID id, ProductDTO product, UUID merchantId) {
        if (product == null || product.name() == null || product.price() <= 0 || product.quantity() < 0) {
            throw new IllegalArgumentException("Invalid product data provided.");
        }

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        if (!existingProduct.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Unauthorized action: User does not own this product.");
        }

        existingProduct.setName(product.name());
        existingProduct.setPrice(product.price());
        existingProduct.setQuantity(product.quantity());
        return productRepository.save(existingProduct);
    }

    public void delete(UUID id, UUID merchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        if (!product.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Unauthorized action: User does not own this product.");
        }

        productRepository.delete(product);
        log.info("Deleted product with ID: {}", id);
    }

    public List<Product> search(String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be null or empty.");
        }
        Optional<List<Product>> allByNameIgnoreCase = productRepository.findAllByNameIgnoreCase(query);
        return allByNameIgnoreCase.orElseThrow(() -> new RuntimeException("No products found matching query: " + query));
    }
}