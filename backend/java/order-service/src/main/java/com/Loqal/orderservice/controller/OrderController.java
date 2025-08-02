package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.services.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            String cachedResponse = redisTemplate.opsForValue().get(cacheKey);

            if (cachedResponse != null) {
                log.info("Idempotency hit for key: {}. Returning cached response.", idempotencyKey);
                try {
                    Order cachedOrder = objectMapper.readValue(cachedResponse, Order.class);
                    // Return OK for a cached response, not CREATED
                    return new ResponseEntity<>(cachedOrder, HttpStatus.OK);
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading cached response.");
                }
            }
        }

        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        try {
            Order createdOrder = orderService.createOrder(orderRequest, userId);

            // Store successful response in cache if key is present
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
                try {
                    String responseToCache = objectMapper.writeValueAsString(createdOrder);
                    // Set a TTL (e.g., 24 hours) to prevent the cache from growing indefinitely
                    redisTemplate.opsForValue().set(cacheKey, responseToCache, Duration.ofHours(24));
                } catch (JsonProcessingException e) {
                    log.error("Failed to cache idempotent response for key: {}", idempotencyKey, e);
                    // Do not fail the request if caching fails, just log it.
                }
            }

            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(orderService.getOrderByIdAndUserId(orderId, userId));
    }

    @DeleteMapping("/{orderId}/cancellation")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        // FIXED: Using UUID for orderId and getting userId from JWT for security
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        try {
            orderService.cancelOrder(orderId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException | SecurityException e) {
            // More specific error handling
            return new ResponseEntity(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/merchant")
    public ResponseEntity<List<Order>> getMerchantOrders(@AuthenticationPrincipal Jwt jwt) {
        // This assumes the JWT contains a tenant_id for merchants
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return ResponseEntity.ok(orderService.getOrdersByMerchantId(merchantId));
    }
}