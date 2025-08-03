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

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return processOrderCreation(orderRequest, jwt, null);
        }

        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        return reactiveRedisTemplate.opsForValue().get(cacheKey)
                .flatMap(cachedResponse -> {
                    log.info("Idempotency hit for key: {}. Returning cached response.", idempotencyKey);
                    return Mono.fromCallable(() -> objectMapper.readValue(cachedResponse, Order.class))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(cachedOrder -> ResponseEntity.ok((Object) cachedOrder));
                })
                .switchIfEmpty(Mono.defer(() -> processOrderCreation(orderRequest, jwt, idempotencyKey)))
                .onErrorResume(e -> {
                    log.error("Error during idempotent order creation for key: {}", idempotencyKey, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during order creation."));
                });
    }


    private Mono<ResponseEntity<Object>> processOrderCreation(OrderRequest orderRequest, Jwt jwt, String idempotencyKey) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));

        return orderService.createOrder(orderRequest, userId)
                .flatMap(createdOrder -> {
                    if (idempotencyKey != null) {
                        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
                        return Mono.fromCallable(() -> objectMapper.writeValueAsString(createdOrder))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(responseToCache ->
                                        reactiveRedisTemplate.opsForValue()
                                                .set(cacheKey, responseToCache, Duration.ofHours(24))
                                )
                                .thenReturn(createdOrder);
                    }
                    return Mono.just(createdOrder);
                })
                .map(finalOrder -> ResponseEntity.status(HttpStatus.CREATED).body((Object) finalOrder));
    }

    // --- Other Endpoints Converted to Reactive (FIXED) ---

    @GetMapping("/my-orders")
    public Mono<ResponseEntity<List<Order>>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
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
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{orderId}/cancellation")
    public Mono<ResponseEntity<Object>> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));

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
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return Mono.fromCallable(() -> orderService.getOrdersByMerchantId(merchantId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}