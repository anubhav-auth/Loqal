package com.loqal.orders.fulfillment;

import com.loqal.orders.dto.OrderStatus;
import com.loqal.orders.entity.Order;
import com.loqal.orders.fulfillment.entity.Agent;
import com.loqal.orders.fulfillment.entity.Delivery;
import com.loqal.orders.fulfillment.repository.AgentRepository;
import com.loqal.orders.fulfillment.repository.DeliveryRepository;
import com.loqal.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class DeliveryServiceTest {

    private DeliveryRepository deliveryRepository;
    private AgentRepository agentRepository;
    private OrderRepository orderRepository;
    private DeliveryService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();
    private Order order;
    private Agent agent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        deliveryRepository = Mockito.mock(DeliveryRepository.class);
        agentRepository = Mockito.mock(AgentRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        service = new DeliveryService(deliveryRepository, agentRepository, orderRepository);

        order = new Order();
        order.setId(orderId);
        order.setCustomerId(UUID.randomUUID());
        order.setCurrentStatus(OrderStatus.ORDER_CONFIRMED);

        agent = new Agent();
        agent.setId(agentId);
        agent.setTenantId(tenantId);
        agent.setStatus(Agent.AVAILABLE);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Mono.empty());
        when(agentRepository.findById(agentId)).thenReturn(Mono.just(agent));
        when(agentRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(deliveryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    void manualAssignCreatesDeliveryAndMarksAgentBusy() {
        StepVerifier.create(service.assign(tenantId, orderId, agentId))
                .assertNext(delivery -> {
                    assertEquals(Delivery.ASSIGNED, delivery.getStatus());
                    assertNotNull(delivery.getPickupOtp());
                    assertEquals(6, delivery.getPickupOtp().length());
                    assertEquals(Agent.ON_DELIVERY, agent.getStatus());
                    assertEquals(OrderStatus.DELIVERY_ASSIGNED, order.getCurrentStatus());
                })
                .verifyComplete();
    }

    @Test
    void doubleAssignmentRejected() {
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Mono.just(new Delivery()));

        StepVerifier.create(service.assign(tenantId, orderId, agentId))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void busyAgentCannotBeAssigned() {
        agent.setStatus(Agent.ON_DELIVERY);

        StepVerifier.create(service.assign(tenantId, orderId, agentId))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void fullLifecycleWithOtpVerification() {
        Delivery assigned = service.assign(tenantId, orderId, agentId).block();

        // wrong OTP rejected
        StepVerifier.create(service.transitionLifecycle(assigned, Delivery.PICKED_UP, "000000"))
                .expectError(IllegalStateException.class)
                .verify();

        // correct OTP advances
        StepVerifier.create(service.transitionLifecycle(assigned, Delivery.PICKED_UP, assigned.getPickupOtp()))
                .assertNext(d -> {
                    assertEquals(Delivery.PICKED_UP, d.getStatus());
                    assertEquals(OrderStatus.ORDER_DISPATCHED, order.getCurrentStatus());
                })
                .verifyComplete();

        StepVerifier.create(service.transitionLifecycle(assigned, Delivery.IN_TRANSIT, null))
                .assertNext(d -> assertEquals(Delivery.IN_TRANSIT, d.getStatus()))
                .verifyComplete();

        // skipping to DELIVERED without in-transit OTP fails
        StepVerifier.create(service.transitionLifecycle(assigned, Delivery.DELIVERED, "000000"))
                .expectError(IllegalStateException.class)
                .verify();

        StepVerifier.create(service.transitionLifecycle(assigned, Delivery.DELIVERED, assigned.getDeliveredOtp()))
                .assertNext(d -> {
                    assertEquals(Delivery.DELIVERED, d.getStatus());
                    assertEquals(OrderStatus.ORDER_DELIVERED, order.getCurrentStatus());
                    assertEquals(Agent.AVAILABLE, agent.getStatus()); // released back
                })
                .verifyComplete();
    }

    @Test
    void geofencedAutoAssignPicksNearestAgent() {
        Agent near = new Agent();
        near.setId(UUID.randomUUID());
        near.setTenantId(tenantId);
        near.setStatus(Agent.AVAILABLE);
        near.setCurrentLat(12.97);
        near.setCurrentLng(77.59); // Bengaluru

        Agent far = new Agent();
        far.setId(UUID.randomUUID());
        far.setTenantId(tenantId);
        far.setStatus(Agent.AVAILABLE);
        far.setCurrentLat(28.61);
        far.setCurrentLng(77.20); // Delhi

        when(agentRepository.findAllByTenantIdAndStatus(tenantId, Agent.AVAILABLE))
                .thenReturn(Flux.just(far, near));
        when(agentRepository.findById(near.getId())).thenReturn(Mono.just(near));

        StepVerifier.create(service.autoAssign(tenantId, orderId, 12.98, 77.60))
                .assertNext(delivery -> assertEquals(near.getId(), delivery.getAgentId()))
                .verifyComplete();
    }

    @Test
    void clockOutWithActiveDeliveryRejected() {
        Agent busy = new Agent();
        busy.setId(UUID.randomUUID());
        busy.setTenantId(tenantId);
        busy.setUserId(UUID.randomUUID());
        busy.setStatus(Agent.ON_DELIVERY);
        when(agentRepository.findByTenantIdAndUserId(any(), any())).thenReturn(Mono.just(busy));

        StepVerifier.create(service.setAvailability(tenantId, busy.getUserId(), false))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
