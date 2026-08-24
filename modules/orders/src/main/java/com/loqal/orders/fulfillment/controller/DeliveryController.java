package com.loqal.orders.fulfillment.controller;

import com.loqal.catalog.exception.ProductNotFoundException;
import com.loqal.orders.entity.Order;
import com.loqal.orders.fulfillment.DeliveryService;
import com.loqal.orders.fulfillment.entity.Agent;
import com.loqal.orders.fulfillment.entity.Delivery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Agent self-service + dispatcher endpoints (PRD §8.2).
 */
@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ---------- agent lifecycle ----------

    public record RegisterAgentRequest(String name, String phone, String vehicleType) {}

    @PostMapping("/agents")
    public Mono<ResponseEntity<Agent>> registerAgent(@RequestBody RegisterAgentRequest req,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        Agent agent = new Agent();
        agent.setUserId(userId);
        agent.setName(req.name());
        agent.setPhone(req.phone());
        agent.setVehicleType(req.vehicleType());
        return deliveryService.registerAgent(tenantId, agent)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @PostMapping("/agents/clock-in")
    public Mono<ResponseEntity<Agent>> clockIn(@AuthenticationPrincipal Jwt jwt) {
        return availability(jwt, true);
    }

    @PostMapping("/agents/clock-out")
    public Mono<ResponseEntity<Agent>> clockOut(@AuthenticationPrincipal Jwt jwt) {
        return availability(jwt, false);
    }

    private Mono<ResponseEntity<Agent>> availability(Jwt jwt, boolean clockIn) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        return deliveryService.setAvailability(tenantId, userId, clockIn)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    public record LocationRequest(double lat, double lng) {}

    @PutMapping("/agents/location")
    public Mono<ResponseEntity<Agent>> updateLocation(@RequestBody LocationRequest req,
                                                      @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        return deliveryService.updateLocation(tenantId, userId, req.lat(), req.lng())
                .map(ResponseEntity::ok);
    }

    // ---------- dispatch (merchant/dispatcher) ----------

    @PostMapping("/orders/{orderId}/assign/{agentId}")
    public Mono<ResponseEntity<Delivery>> assignManual(@PathVariable UUID orderId,
                                                       @PathVariable UUID agentId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return deliveryService.assign(tenantId, orderId, agentId)
                .map(d -> ResponseEntity.status(HttpStatus.CREATED).body(redact(d)))
                .onErrorResume(ProductNotFoundException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().build()))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    @PostMapping("/orders/{orderId}/auto-assign")
    public Mono<ResponseEntity<Delivery>> assignAuto(@PathVariable UUID orderId,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return deliveryService.autoAssign(tenantId, orderId)
                .map(d -> ResponseEntity.status(HttpStatus.CREATED).body(redact(d)))
                .onErrorResume(ProductNotFoundException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    /** Pickup OTP for dispatcher handover. */
    @GetMapping("/orders/{orderId}/pickup-otp")
    public Mono<ResponseEntity<String>> pickupOtp(@PathVariable UUID orderId,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return deliveryService.getByOrderId(orderId)
                .map(Delivery::getPickupOtp)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/agents/available")
    public Flux<Agent> availableAgents(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        return deliveryService.availableAgents(tenantId);
    }

    // ---------- agent transitions ----------

    public record TransitionRequest(String status, String otp) {}

    @PostMapping("/agent/transition")
    public Mono<ResponseEntity<Delivery>> transition(@RequestBody TransitionRequest req,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        return deliveryService.transition(userId, req.status(), req.otp())
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().build()));
    }

    // ---------- tracking / queries ----------

    @GetMapping("/orders/{orderId}")
    public Mono<ResponseEntity<Delivery>> trackingByOrder(@PathVariable UUID orderId) {
        return deliveryService.getByOrderId(orderId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/agent/deliveries")
    public Flux<Delivery> myDeliveries(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        return deliveryService.deliveriesForAgent(userId);
    }

    static Delivery redact(Delivery d) {
        d.setPickupOtp(null);
        d.setDeliveredOtp(null);
        return d;
    }
}
