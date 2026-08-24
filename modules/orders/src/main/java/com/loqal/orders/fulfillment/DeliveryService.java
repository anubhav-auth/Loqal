package com.loqal.orders.fulfillment;

import com.loqal.orders.dto.OrderStatus;
import com.loqal.catalog.exception.ProductNotFoundException;
import com.loqal.orders.entity.Order;
import com.loqal.orders.fulfillment.entity.Agent;
import com.loqal.orders.fulfillment.entity.Delivery;
import com.loqal.orders.fulfillment.repository.AgentRepository;
import com.loqal.orders.fulfillment.repository.DeliveryRepository;
import com.loqal.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

/**
 * Delivery assignment & lifecycle (PRD §8.2).
 * States: ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED | FAILED.
 * Handover from dispatcher to agent requires the pickup OTP (PRD ORD flow).
 */
@Service
public class DeliveryService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final DeliveryRepository deliveryRepository;
    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           AgentRepository agentRepository,
                           OrderRepository orderRepository) {
        this.deliveryRepository = deliveryRepository;
        this.agentRepository = agentRepository;
        this.orderRepository = orderRepository;
    }

    // ---------- agent availability ----------

    public Mono<Agent> registerAgent(UUID tenantId, Agent agent) {
        agent.setTenantId(tenantId);
        agent.setId(UUID.randomUUID());
        agent.markNew();
        agent.setStatus(Agent.OFF_DUTY);
        agent.setCreatedAt(LocalDateTime.now());
        return agentRepository.save(agent);
    }

    public Mono<Agent> setAvailability(UUID tenantId, UUID userId, boolean clockIn) {
        return agentRepository.findByTenantIdAndUserId(tenantId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agent profile not found")))
                .flatMap(agent -> {
                    if (!Agent.OFF_DUTY.equals(agent.getStatus()) && !clockIn) {
                        return Mono.error(new IllegalStateException("Finish active deliveries before clocking out"));
                    }
                    agent.setStatus(clockIn ? Agent.AVAILABLE : Agent.OFF_DUTY);
                    agent.setUpdatedAt(LocalDateTime.now());
                    return agentRepository.save(agent);
                });
    }

    public Mono<Agent> updateLocation(UUID tenantId, UUID userId, double lat, double lng) {
        return agentRepository.findByTenantIdAndUserId(tenantId, userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agent profile not found")))
                .flatMap(agent -> {
                    agent.setCurrentLat(lat);
                    agent.setCurrentLng(lng);
                    agent.setUpdatedAt(LocalDateTime.now());
                    return agentRepository.save(agent); // tracking breadcrumb (latest position)
                });
    }

    public Flux<Agent> availableAgents(UUID tenantId) {
        return agentRepository.findAllByTenantIdAndStatus(tenantId, Agent.AVAILABLE);
    }

    // ---------- dispatch ----------

    /** Manual dispatch: dispatcher picks a specific agent. */
    public Mono<Delivery> assign(UUID tenantId, UUID orderId, UUID agentId) {
        return assignInternal(tenantId, orderId, Mono.just(agentId));
    }

    /**
     * Auto-dispatch with geofencing (PRD §8.2): picks the AVAILABLE agent
     * nearest to the given destination coordinates (haversine). Agents without
     * a known location sort last; falls back to deterministic ordering when
     * no coordinates are supplied.
     */
    public Mono<Delivery> autoAssign(UUID tenantId, UUID orderId, Double lat, Double lng) {
        Comparator<Agent> byDistance = (lat != null && lng != null)
                ? Comparator.comparing(a -> a.distanceKmTo(lat, lng))
                : Comparator.comparing(Agent::getId);
        Mono<UUID> nearestAgentId = agentRepository.findAllByTenantIdAndStatus(tenantId, Agent.AVAILABLE)
                .sort(byDistance)
                .take(1)
                .map(Agent::getId)
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(new IllegalStateException("No available agents")));
        return assignInternal(tenantId, orderId, nearestAgentId);
    }

    private Mono<Delivery> assignInternal(UUID tenantId, UUID orderId, Mono<UUID> agentIdMono) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(orderId)))
                .filter(order -> order.getCustomerId() != null) // sanity
                .flatMap(order -> deliveryRepository.findByOrderId(orderId)
                        .flatMap(existing -> Mono.<Delivery>error(
                                new IllegalStateException("Order already has an active delivery")))
                        .switchIfEmpty(Mono.defer(() -> agentIdMono.flatMap(agentId ->
                                createDelivery(tenantId, order, agentId)))));
    }

    private Mono<Delivery> createDelivery(UUID tenantId, Order order, UUID agentId) {
        return agentRepository.findById(agentId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agent not found for this tenant")))
                .filter(a -> Agent.AVAILABLE.equals(a.getStatus()))
                .switchIfEmpty(Mono.error(new IllegalStateException("Agent is not available")))
                .flatMap(agent -> {
                    Delivery delivery = new Delivery();
                    delivery.setId(UUID.randomUUID());
                    delivery.setTenantId(tenantId);
                    delivery.setOrderId(order.getId());
                    delivery.setAgentId(agent.getId());
                    delivery.setStatus(Delivery.ASSIGNED);
                    delivery.setPickupOtp(generateOtp());
                    delivery.setDeliveredOtp(generateOtp());
                    delivery.setAssignedAt(LocalDateTime.now());
                    delivery.setCreatedAt(LocalDateTime.now());
                    delivery.markNew();

                    agent.setStatus(Agent.ON_DELIVERY);
                    agent.setUpdatedAt(LocalDateTime.now());

                    order.setCurrentStatus(OrderStatus.DELIVERY_ASSIGNED);
                    order.setUpdatedAt(LocalDateTime.now());

                    return agentRepository.save(agent)
                            .then(orderRepository.save(order))
                            .then(deliveryRepository.save(delivery));
                });
    }

    // ---------- lifecycle ----------

    public Mono<Delivery> transition(UUID agentUserId, String targetStatus, String otp) {
        return agentRepository.findByUserId(agentUserId)
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agent profile not found")))
                .flatMap(agent -> deliveryRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getId())
                        .next()
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("No active delivery for agent"))))
                .flatMap(delivery -> applyTransition(delivery, targetStatus, otp));
    }

    /** Package-private for tests: apply a transition to an explicit delivery. */
    Mono<Delivery> transitionLifecycle(Delivery delivery, String targetStatus, String otp) {
        return applyTransition(delivery, targetStatus, otp);
    }

    private Mono<Delivery> applyTransition(Delivery delivery, String targetStatus, String otp) {
        LocalDateTime now = LocalDateTime.now();
        boolean valid;
        switch (targetStatus) {
            case Delivery.PICKED_UP:
                valid = Delivery.ASSIGNED.equals(delivery.getStatus()) && otpMatches(delivery.getPickupOtp(), otp);
                if (valid) delivery.setPickedUpAt(now);
                break;
            case Delivery.IN_TRANSIT:
                valid = Delivery.PICKED_UP.equals(delivery.getStatus());
                break;
            case Delivery.DELIVERED:
                valid = Delivery.IN_TRANSIT.equals(delivery.getStatus()) && otpMatches(delivery.getDeliveredOtp(), otp);
                if (valid) delivery.setDeliveredAt(now);
                break;
            case Delivery.FAILED:
                valid = !Delivery.DELIVERED.equals(delivery.getStatus())
                        && !Delivery.FAILED.equals(delivery.getStatus());
                break;
            default:
                valid = false;
        }
        if (!valid) {
            return Mono.error(new IllegalStateException(
                    "Cannot transition from " + delivery.getStatus() + " to " + targetStatus));
        }
        delivery.setStatus(targetStatus);
        delivery.setUpdatedAt(now);

        return finalizeSideEffects(delivery).then(deliveryRepository.save(delivery));
    }

    private Mono<Void> finalizeSideEffects(Delivery delivery) {
        return agentRepository.findById(delivery.getAgentId())
                .flatMap(agent -> {
                    if (Delivery.DELIVERED.equals(delivery.getStatus())
                            || Delivery.FAILED.equals(delivery.getStatus())) {
                        agent.setStatus(Agent.AVAILABLE);
                        agent.setUpdatedAt(LocalDateTime.now());
                        return agentRepository.save(agent).then();
                    }
                    return Mono.empty();
                })
                .then(orderRepository.findById(delivery.getOrderId())
                        .flatMap(order -> {
                            switch (delivery.getStatus()) {
                                case Delivery.PICKED_UP:
                                    order.setCurrentStatus(OrderStatus.ORDER_DISPATCHED);
                                    break;
                                case Delivery.DELIVERED:
                                    order.setCurrentStatus(OrderStatus.ORDER_DELIVERED);
                                    break;
                                case Delivery.FAILED:
                                    order.setCurrentStatus(OrderStatus.DELIVERY_FAILED);
                                    break;
                                default:
                                    return Mono.empty();
                            }
                            order.setUpdatedAt(LocalDateTime.now());
                            return orderRepository.save(order).then();
                        }));
    }

    public Flux<Delivery> deliveriesForAgent(UUID agentUserId) {
        return agentRepository.findByUserId(agentUserId)
                .next()
                .flatMapMany(agent -> deliveryRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getId()));
    }

    public Mono<Delivery> getByOrderId(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(orderId)));
    }

    // ---------- helpers ----------

    static String generateOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    static boolean otpMatches(String expected, String provided) {
        return expected != null && expected.equals(provided == null ? "" : provided.trim());
    }
}
