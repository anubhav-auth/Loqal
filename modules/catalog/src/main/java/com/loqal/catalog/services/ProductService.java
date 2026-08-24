package com.loqal.catalog.services;

import com.loqal.catalog.api.ProductApi;
import com.loqal.catalog.dto.UpdateStockRequestDto;
import com.loqal.contracts.events.OrderCancellationEvent;
import com.loqal.contracts.events.StockReservationRequest;
import com.loqal.contracts.events.StockReservationResponse;
import com.loqal.catalog.entity.ProcessedEvent;
import com.loqal.catalog.entity.Product;
import com.loqal.catalog.entity.ProductDTO;
import com.loqal.contracts.events.ProductOrderRequest;
import com.loqal.catalog.exception.InsufficientStockException;
import com.loqal.catalog.exception.ProductNotFoundException;
import com.loqal.catalog.exception.UnauthorizedProductAccessException;
import com.loqal.catalog.repository.ProcessedEventRepository;
import com.loqal.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService implements ProductApi {

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Bridge until the schema migration (Phase 2a): the products table stores
     * price as double precision; the API contract exposes integer minor units.
     */
    @Override
    public Mono<ProductPrice> findPrice(UUID productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(productId)))
                .map(product -> new ProductPrice(
                        product.getId(),
                        Math.round(product.getPrice() * 100),
                        product.getQuantity(),
                        true));
    }

    @Value("${spring.kafka.topic.order-cancel}")
    private String orderCancellationTopic;

    @Value("${spring.kafka.topic.stock-reservation-result}")
    private String stockReservationResultTopic;

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-creation-requested}", groupId = "product-service-group")
    public void consumeOrderCreationRequest(StockReservationRequest request) {
        Mono.defer(() -> processedEventRepository.findById(request.getOrderId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalEvent -> {
                    if (optionalEvent.isPresent()) {
                        // Event exists: handle duplicate request
                        ProcessedEvent event = optionalEvent.get();
                        log.warn("Duplicate request for order {}. Status: {}. Resending original result.", request.getOrderId(), event.getStatus());
                        StockReservationResponse originalResponse = new StockReservationResponse();
                        originalResponse.setOrderId(event.getOrderId());
                        originalResponse.setStatus(event.getStatus());
                        if (event.getReason() != null) {
                            originalResponse.setReason(event.getReason());
                        }
                        return Mono.fromFuture(kafkaTemplate.send(stockReservationResultTopic, originalResponse)).then(); // Return Mono<Void>
                    } else {
                        return processOrderCreation(request);
                    }
                })
        ).subscribe();
    }

    private Mono<Void> processOrderCreation(StockReservationRequest request) {
        StockReservationResponse response = new StockReservationResponse();
        response.setOrderId(request.getOrderId());

        return Flux.fromIterable(request.getItems())
                .sort(Comparator.comparing(ProductOrderRequest::getProductId))
                .concatMap(item -> productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .switchIfEmpty(Mono.error(new ProductNotFoundException(item.getProductId())))
                        .flatMap(product -> {
                            if (product.getQuantity() < item.getQuantity()) {
                                return Mono.error(new InsufficientStockException("Insufficient stock for product ID: " + product.getId()));
                            }
                            product.setQuantity(product.getQuantity() - item.getQuantity());
                            return productRepository.save(product);
                        }))
                .then(Mono.defer(() -> {
                    response.setStatus("SUCCESS");
                    log.info("Stock successfully reserved for order {}", request.getOrderId());
                    ProcessedEvent event = new ProcessedEvent(request.getOrderId(), "SUCCESS", null);
                    return processedEventRepository.save(event);
                }))
                .doOnSuccess(event -> kafkaTemplate.send(stockReservationResultTopic, response))
                .onErrorResume(e -> {
                    log.error("Stock reservation failed for order {}: {}", request.getOrderId(), e.getMessage());
                    response.setStatus("FAILED");
                    response.setReason(e.getMessage());
                    ProcessedEvent event = new ProcessedEvent(request.getOrderId(), "FAILED", e.getMessage());
                    return processedEventRepository.save(event)
                            .doOnSuccess(savedEvent -> kafkaTemplate.send(stockReservationResultTopic, response))
                            .then(Mono.empty());
                })
                .then();
    }


    @KafkaListener(topics = "${spring.kafka.topic.order-cancel-dlt}", groupId = "product-service-dlt-group")
    public void consumeOrderCancellationDLT(ConsumerRecord<String, Object> record) {
        log.error("🚨 DEAD LETTER QUEUE 🚨 | Received a failed message from topic: {}", record.topic());
        log.error("Payload: {}", record.value());
    }

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.order-cancel}", groupId = "product-service-group")
    public void consumeOrderCancellation(OrderCancellationEvent request) {
        processedEventRepository.findById(request.getOrderId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalEvent -> {
                    if (optionalEvent.isPresent() && "CANCELLED".equals(optionalEvent.get().getStatus())) {
                        log.warn("Duplicate cancellation request for order {}. Ignoring.", request.getOrderId());
                        return Mono.empty();
                    } else {
                        return processOrderCancellation(request);
                    }
                })
                .subscribe();
    }

    private Mono<Void> processOrderCancellation(OrderCancellationEvent request) {
        return Flux.fromIterable(request.getItems())
                .sort(Comparator.comparing(ProductOrderRequest::getProductId))
                .concatMap(item -> productRepository.findByIdWithPessimisticLock(item.getProductId())
                        .switchIfEmpty(Mono.error(new ProductNotFoundException(item.getProductId())))
                        .flatMap(product -> {
                            product.setQuantity(product.getQuantity() + item.getQuantity());
                            return productRepository.save(product);
                        }))
                .then(Mono.defer(() -> {
                    ProcessedEvent event = new ProcessedEvent(request.getOrderId(), "CANCELLED", null);
                    return processedEventRepository.save(event);
                }))
                .doOnError(e -> log.error("Failed to process order cancellation event. Error: {}. This may require manual intervention.", e.getMessage()))
                .then();
    }

    public Mono<Product> create(ProductDTO product, UUID merchantId) {
        Product newProduct = new Product();
        newProduct.setName(product.name());
        newProduct.setDescription(product.description());
        newProduct.setCategory_name(product.category().getCategory_name());
        newProduct.setCategory_description(product.category().getCategory_description());
        newProduct.setPrice(product.price());
        newProduct.setQuantity(product.quantity());
        newProduct.setImage_urls(product.image_urls());
        newProduct.setCreated_at(LocalDateTime.now());
        newProduct.setMerchantId(merchantId);
        return productRepository.save(newProduct);
    }

    public Flux<Product> getAllByMerchant(UUID tenantId) {
        return productRepository.findAllByMerchantId(tenantId);
    }

    public Mono<Product> getById(UUID id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    public Mono<Product> update(UUID id, UUID merchantId, UpdateStockRequestDto product) {
        return getById(id)
                .flatMap(existingProduct -> {
                    if (!existingProduct.getMerchantId().equals(merchantId)) {
                        return Mono.error(new UnauthorizedProductAccessException());
                    }
                    existingProduct.setQuantity(existingProduct.getQuantity() + product.newStock());
                    return productRepository.save(existingProduct);
                });
    }

    public Mono<Void> delete(UUID id, UUID merchantId) {
        return getById(id)
                .flatMap(product -> {
                    if (!product.getMerchantId().equals(merchantId)) {
                        return Mono.error(new UnauthorizedProductAccessException());
                    }
                    return productRepository.delete(product);
                })
                .doOnSuccess(v -> log.info("Deleted product with ID: {}", id));
    }

    public Flux<Product> search(String query) {
        if (query == null || query.isBlank()) {
            return Flux.empty();
        }
        return productRepository.findAllByNameIgnoreCase(query);
    }
}
