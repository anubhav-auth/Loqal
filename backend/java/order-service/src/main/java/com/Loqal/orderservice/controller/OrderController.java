package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private final OrderService orderService;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    // Constructor injection
    public OrderController(OrderService orderService, ReactiveStringRedisTemplate reactiveRedisTemplate, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // If no idempotency key, proceed directly to order creation.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return processOrderCreation(orderRequest, jwt, null);
        }

        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        // Reactive Idempotency Check
        return reactiveRedisTemplate.opsForValue().get(cacheKey)
                .flatMap(cachedResponse -> {
                    log.info("Idempotency hit for key: {}. Returning cached response.", idempotencyKey);
                    // The response is cached as a JSON string, deserialize it.
                    // Deserialization is a blocking call, so we wrap it to avoid blocking the event loop.
                    return Mono.fromCallable(() -> objectMapper.readValue(cachedResponse, Order.class))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(cachedOrder -> ResponseEntity.ok((Object) cachedOrder));
                })
                // If the key is not in the cache (switchIfEmpty), proceed with creating the order.
                .switchIfEmpty(Mono.defer(() -> processOrderCreation(orderRequest, jwt, idempotencyKey)))
                .onErrorResume(e -> {
                    log.error("Error during idempotent order creation for key: {}", idempotencyKey, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during order creation."));
                });
    }

    /**
     * Handles the core logic of creating an order and caching the response if an idempotency key is provided.
     */
    private Mono<ResponseEntity<Object>> processOrderCreation(OrderRequest orderRequest, Jwt jwt, String idempotencyKey) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub")); // "sub" is the standard claim for user ID

        // Call the reactive service method. NO .block() here.
        return orderService.createOrder(orderRequest, userId)
                .flatMap(createdOrder -> {
                    // If an idempotency key was used, cache the successful response.
                    if (idempotencyKey != null) {
                        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
                        // Serialization is blocking, so wrap it.
                        return Mono.fromCallable(() -> objectMapper.writeValueAsString(createdOrder))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(responseToCache ->
                                        reactiveRedisTemplate.opsForValue()
                                                .set(cacheKey, responseToCache, Duration.ofHours(24))
                                )
                                // Return the createdOrder regardless of whether caching succeeded.
                                .thenReturn(createdOrder);
                    }
                    return Mono.just(createdOrder);
                })
                // Map the final Order object to a ResponseEntity.
                .map(finalOrder -> ResponseEntity.status(HttpStatus.CREATED).body((Object) finalOrder));
    }

    // --- Other Endpoints Converted to Reactive (FIXED) ---

    @GetMapping("/my-orders")
    public Mono<ResponseEntity<List<Order>>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        // Assumes service returns a Flux<Order>, which we collect into a List.
        return Mono.fromCallable(() -> orderService.getOrdersByUserId(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return Mono.fromCallable(() -> orderService.getOrderByIdAndUserId(orderId, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(order -> ResponseEntity.ok(order))
                // If the callable throws an exception (e.g., order not found from orElseThrow), catch it and return 404.
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{orderId}/cancellation")
    public Mono<ResponseEntity<Object>> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        // Assumes service returns Mono<Void>
        return Mono.fromRunnable(() -> orderService.cancelOrder(orderId, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.noContent().build()))
                .onErrorResume(SecurityException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage())))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage())));
    }

    @GetMapping("/merchant")
    public Mono<ResponseEntity<List<Order>>> getMerchantOrders(@AuthenticationPrincipal Jwt jwt) {
        // This assumes the JWT contains a tenant_id for merchants
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        // Assumes service returns a Flux<Order>
        return Mono.fromCallable(() -> orderService.getOrdersByMerchantId(merchantId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}