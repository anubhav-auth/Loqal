package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.dto.OrderStatus;
import com.Loqal.orderservice.dto.OrderUpdate;
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
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Order createdOrder = orderService.createOrder(orderRequest, idempotencyKey);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }


    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        Order orderByIdAndUserId = (Order) orderService.getOrderByIdAndUserId(orderId, userId);
        return ResponseEntity.ok(orderByIdAndUserId);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        Order updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @DeleteMapping("/{id}/cancellation")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.processOrderCancellation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/merchant")
    public ResponseEntity<List<Order>> getMerchantOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return ResponseEntity.ok(orderService.getOrdersByMerchantId(merchantId));
    }
}