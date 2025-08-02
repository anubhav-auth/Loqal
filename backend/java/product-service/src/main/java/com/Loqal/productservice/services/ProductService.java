package com.Loqal.productservice.services;

import com.Loqal.productservice.dto.OrderEvent;
import com.Loqal.productservice.dto.OrderStatus;
import com.Loqal.productservice.dto.OrderStatusUpdate;
import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.exception.InsufficientStockException;
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
    public void reserveStockForOrder(List<ProductOrderRequest> orderRequests) {
        for (ProductOrderRequest item : orderRequests) {
            // The pessimistic lock is applied by the repository method.
            Product product = productRepository.findByIdWithPessimisticLock(item.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getId()));

            if (product.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
            log.info("Reserved {} units of stock for product {}. New stock: {}", item.getQuantity(), product.getName(), product.getQuantity());
        }
    }

    /**
     * Reverts stock for a cancelled order. This also needs to be transactional.
     */
    @Transactional
    public void revertStockForCancelledOrder(List<ProductOrderRequest> orderRequests) {
        for (ProductOrderRequest item : orderRequests) {
            Product product = productRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getId())); // Or handle more gracefully

            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }



    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-cancel}", groupId = "product-service-group")
    public void consumeOrderCancellation(List<ProductOrderRequest> itemsToRevert) {
        log.info("Received order cancellation event to revert stock.");
        try {
            for (ProductOrderRequest item : itemsToRevert) {
                Product product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
                log.info("Reverted {} units of stock for cancelled order. Product: {}. New stock: {}", item.getQuantity(), product.getName(), product.getQuantity());
            }
        } catch (Exception e) {
            // If this fails, the message will be re-processed by Kafka.
            // A Dead Letter Queue (DLQ) is needed here for production to handle repeated failures.
            log.error("Failed to process order cancellation event. Error: {}", e.getMessage());
            throw e; // Re-throw the exception to trigger Kafka's retry mechanism.
        }
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