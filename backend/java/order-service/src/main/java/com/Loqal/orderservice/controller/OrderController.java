package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // NOTE: Idempotency logic is not implemented but the key is accepted.
        // A full implementation would check this key against a cache (e.g., Redis) before processing.

        UUID userId = UUID.fromString(jwt.getClaimAsString("sub")); // 'sub' is standard for user ID
        try {
            Order createdOrder = orderService.createOrder(orderRequest, userId);
            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (Exception e) {
            // This will catch both stock reservation failures and order save failures
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
        // Exception handling can be done here or with a @ControllerAdvice
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