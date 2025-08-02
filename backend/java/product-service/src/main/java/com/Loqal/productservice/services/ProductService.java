package com.Loqal.productservice.services;

import com.Loqal.productservice.dto.events.OrderCreationRequest;
import com.Loqal.productservice.dto.events.StockReservationResponse;
import com.Loqal.productservice.entity.ProcessedEvent;
import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.exception.InsufficientStockException;
import com.Loqal.productservice.repository.ProcessedEventRepository;
import com.Loqal.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.order-cancel}") // Standardized topic name
    private String orderCancellationTopic;

    @Value("${spring.kafka.topic.stock-reservation-result}")
    private String stockReservationResultTopic;

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-creation-requested}", groupId = "product-service-group")
    public void consumeOrderCreationRequest(OrderCreationRequest request) {

        log.info("Received stock reservation request for order {}", request.getOrderId());
        StockReservationResponse response = new StockReservationResponse();
        response.setOrderId(request.getOrderId());

        try {
            if (processedEventRepository.existsById(request.getOrderId())) {
                log.warn("Duplicate request for order {}, ignoring.", request.getOrderId());
                return;
            }
            processedEventRepository.save(new ProcessedEvent(request.getOrderId()));
            List<ProductOrderRequest> sortedItems = request.getItems().stream()
                    .sorted(Comparator.comparing(ProductOrderRequest::getProductId))
                    .toList();

            for (ProductOrderRequest item : sortedItems) {
                Product product = productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

                if (product.getQuantity() < item.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock for product ID: " + product.getId());
                }

                product.setQuantity(product.getQuantity() - item.getQuantity());
                productRepository.save(product);
            }

            response.setStatus("SUCCESS");
            log.info("Stock successfully reserved for order {}", request.getOrderId());


        } catch (Exception e) {
            // IMPORTANT: The transaction will roll back, undoing any partial stock changes.
            log.error("Stock reservation failed for order {}: {}", request.getOrderId(), e.getMessage());
            response.setStatus("FAILED");
            response.setReason(e.getMessage());
            response.setReason(e.getMessage());
            // re-throw the exception to ensure the transaction rolls back.
            // The afterCompletion hook will still run.
            throw e;
        } finally {
            // Use afterCompletion to guarantee a response is sent.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        // Only send SUCCESS on actual commit.
                        kafkaTemplate.send(stockReservationResultTopic, response);
                    } else if (status == TransactionSynchronization.STATUS_ROLLED_BACK && "FAILED".equals(response.getStatus())) {
                        // Only send FAILED on actual rollback.
                        kafkaTemplate.send(stockReservationResultTopic, response);
                    }
                }
            });
        }
    }

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
        return productRepository.findAllByMerchantId(tenantId);
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