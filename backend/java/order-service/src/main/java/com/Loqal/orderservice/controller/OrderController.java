package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.dto.OrderDto;
import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.events.OrderCreationResponse;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
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
    public Mono<ResponseEntity<OrderCreationResponse>> createOrder(
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
                    return Mono.fromCallable(() -> objectMapper.readValue(cachedResponse, OrderCreationResponse.class))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(ResponseEntity::ok);
                })
                .switchIfEmpty(Mono.defer(() -> processOrderCreation(orderRequest, jwt, idempotencyKey)))
                .onErrorResume(e -> {
                    log.error("Error during idempotent order creation for key: {}", idempotencyKey, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    private Mono<ResponseEntity<OrderCreationResponse>> processOrderCreation(OrderRequest orderRequest, Jwt jwt, String idempotencyKey) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));


        return orderService.createOrder(orderRequest, userId)
                .flatMap(creationResponse -> {
                    if (idempotencyKey != null) {
                        String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
                        return Mono.fromCallable(() -> objectMapper.writeValueAsString(creationResponse))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(responseToCache ->
                                        reactiveRedisTemplate.opsForValue()
                                                .set(cacheKey, responseToCache, Duration.ofHours(24))
                                )
                                .thenReturn(creationResponse);
                    }
                    return Mono.just(creationResponse);
                })
                .map(finalResponse -> ResponseEntity.status(HttpStatus.CREATED).body(finalResponse));
    }

    @GetMapping("/my-orders")
    public Flux<Order> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return orderService.getOrdersByUserId(userId);
    }

    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return orderService.getOrderByIdAndUserId(orderId, userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{orderId}/cancellation")
    public Mono<ResponseEntity<Void>> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));

        return orderService.cancelOrder(orderId, userId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(SecurityException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()))
                .onErrorResume(RuntimeException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @GetMapping("/merchant/{merchantId}")
    public Flux<Order> getMerchantOrders(@PathVariable UUID merchantId) {

        return orderService.getOrdersByMerchantId(merchantId);
    }

    

}