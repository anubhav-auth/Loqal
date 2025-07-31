package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest orderRequest, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        Order newOrder = orderService.createOrder(orderRequest, userId);
        return ResponseEntity.ok(newOrder);
    }


//    @GetMapping
//    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
//        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
//        List<Order> orders = orderService.getOrdersByUserId(userId);
//        return ResponseEntity.ok(orders);
//    }


//    @GetMapping("/{orderId}")
//    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
//        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
//        return orderService.getOrderByIdAndUserId(orderId, userId)
//                .map(ResponseEntity.ok().body("Order found"))
//                .orElse(ResponseEntity.notFound().build());
//    }
}