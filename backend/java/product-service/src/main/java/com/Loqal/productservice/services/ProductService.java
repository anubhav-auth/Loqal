package com.Loqal.productservice.services;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.exception.InsufficientStockException;
import com.Loqal.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Value("${spring.kafka.topic.order-cancel}") // Standardized topic name
    private String orderCancellationTopic;

    @Transactional
    public void reserveStockForOrder(List<ProductOrderRequest> orderRequests) {
        for (ProductOrderRequest item : orderRequests) {
            Product product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

            if (product.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
            log.info("Reserved {} units for product {}. New stock: {}", item.getQuantity(), product.getName(), product.getQuantity());
        }
    }

    /**
     * REFACTORED: Combined stock revert logic into a single, reliable Kafka listener.
     * The synchronous endpoint was removed to prevent inconsistencies.
     */
    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-cancel}", groupId = "product-service-group")
    public void consumeOrderCancellation(List<ProductOrderRequest> itemsToRevert) {
        log.info("Received event to revert stock for a cancelled order.");
        try {
            for (ProductOrderRequest item : itemsToRevert) {
                Product product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
                log.info("Reverted {} units of stock for product {}. New stock: {}", item.getQuantity(), product.getName(), product.getQuantity());
            }
        } catch (Exception e) {
            log.error("Failed to process order cancellation event. Error: {}. This may require manual intervention.", e.getMessage());
            // Re-throwing will trigger Kafka retries. Configure a Dead Letter Queue (DLQ) for production.
            throw e;
        }
    }

    // --- Standard CRUD and Search ---

    public Product create(ProductDTO product, UUID merchantId) {
        // ... (your existing validation logic is good)
        Product newProduct = new Product();
        newProduct.setName(product.name());
        newProduct.setPrice(product.price());
        newProduct.setQuantity(product.quantity());
        newProduct.setMerchantId(merchantId);
        return productRepository.save(newProduct);
    }

    public List<Product> getAllByMerchant(UUID tenantId) {
        // REFACTORED: Return an empty list instead of throwing an exception.
        return productRepository.findAllByMerchantId(tenantId).orElse(Collections.emptyList());
    }

    public Product getById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

    public Product update(UUID id, ProductDTO product, UUID merchantId) {
        // ... (your existing update logic with ownership check is good)
        Product existingProduct = getById(id); // Re-use getById for DRY principle

        if (!existingProduct.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Unauthorized action: User does not own this product.");
        }

        existingProduct.setName(product.name());
        existingProduct.setPrice(product.price());
        existingProduct.setQuantity(product.quantity());
        return productRepository.save(existingProduct);
    }

    public void delete(UUID id, UUID merchantId) {
        Product product = getById(id);
        if (!product.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Unauthorized action: User does not own this product.");
        }
        productRepository.delete(product);
        log.info("Deleted product with ID: {}", id);
    }

    public List<Product> search(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        return productRepository.findAllByNameIgnoreCase(query).orElse(Collections.emptyList());
    }
}