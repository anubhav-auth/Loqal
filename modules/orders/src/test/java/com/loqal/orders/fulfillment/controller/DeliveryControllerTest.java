package com.loqal.orders.fulfillment.controller;

import com.loqal.orders.fulfillment.DeliveryService;
import com.loqal.orders.fulfillment.controller.DeliveryController.AutoAssignRequest;
import com.loqal.orders.fulfillment.controller.DeliveryController.LocationRequest;
import com.loqal.orders.fulfillment.controller.DeliveryController.RegisterAgentRequest;
import com.loqal.orders.fulfillment.controller.DeliveryController.TransitionRequest;
import com.loqal.orders.fulfillment.entity.Agent;
import com.loqal.orders.fulfillment.entity.Delivery;
import com.loqal.catalog.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    private DeliveryController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID AGENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new DeliveryController(deliveryService);
    }

    private Jwt mockJwt() {
        Jwt jwt = org.mockito.Mockito.mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn(TENANT_ID.toString());
        when(jwt.getClaimAsString("user_id")).thenReturn(USER_ID.toString());
        return jwt;
    }

    private Agent sampleAgent() {
        Agent a = new Agent();
        a.setId(AGENT_ID);
        a.setTenantId(TENANT_ID);
        a.setUserId(USER_ID);
        a.setName("Test Agent");
        a.setPhone("9999999999");
        a.setVehicleType("BIKE");
        a.setStatus(Agent.AVAILABLE);
        a.markNew();
        return a;
    }

    private Delivery sampleDelivery() {
        Delivery d = new Delivery();
        d.setId(UUID.randomUUID());
        d.setTenantId(TENANT_ID);
        d.setOrderId(ORDER_ID);
        d.setAgentId(AGENT_ID);
        d.setStatus(Delivery.ASSIGNED);
        d.setPickupOtp("123456");
        d.setDeliveredOtp("654321");
        d.setAssignedAt(LocalDateTime.now());
        d.markNew();
        return d;
    }

    @Test
    void registerAgent_success_returns201() {
        Agent agent = sampleAgent();
        when(deliveryService.registerAgent(eq(TENANT_ID), any(Agent.class)))
                .thenReturn(Mono.just(agent));

        StepVerifier.create(controller.registerAgent(
                        new RegisterAgentRequest("Test Agent", "9999999999", "BIKE"), mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().value()).isEqualTo(201);
                    assertThat(r.getBody().getName()).isEqualTo("Test Agent");
                })
                .verifyComplete();
    }

    @Test
    void clockIn_success_returns200() {
        Agent agent = sampleAgent();
        agent.setStatus(Agent.AVAILABLE);
        when(deliveryService.setAvailability(eq(TENANT_ID), eq(USER_ID), eq(true)))
                .thenReturn(Mono.just(agent));

        StepVerifier.create(controller.clockIn(mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().getStatus()).isEqualTo(Agent.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    void clockIn_agentNotFound_returns404() {
        when(deliveryService.setAvailability(eq(TENANT_ID), eq(USER_ID), eq(true)))
                .thenReturn(Mono.error(new IllegalArgumentException("Agent profile not found")));

        StepVerifier.create(controller.clockIn(mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(404))
                .verifyComplete();
    }

    @Test
    void clockOut_activeDeliveries_returns409() {
        when(deliveryService.setAvailability(eq(TENANT_ID), eq(USER_ID), eq(false)))
                .thenReturn(Mono.error(new IllegalStateException("Finish active deliveries")));

        StepVerifier.create(controller.clockOut(mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(409))
                .verifyComplete();
    }

    @Test
    void updateLocation_success_returns200() {
        Agent agent = sampleAgent();
        agent.setCurrentLat(12.97);
        agent.setCurrentLng(77.59);
        when(deliveryService.updateLocation(eq(TENANT_ID), eq(USER_ID), eq(12.97), eq(77.59)))
                .thenReturn(Mono.just(agent));

        StepVerifier.create(controller.updateLocation(new LocationRequest(12.97, 77.59), mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().getCurrentLat()).isEqualTo(12.97);
                })
                .verifyComplete();
    }

    @Test
    void assignManual_success_returns201() {
        Delivery delivery = sampleDelivery();
        when(deliveryService.assign(eq(TENANT_ID), eq(ORDER_ID), eq(AGENT_ID)))
                .thenReturn(Mono.just(delivery));

        StepVerifier.create(controller.assignManual(ORDER_ID, AGENT_ID, mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().value()).isEqualTo(201);
                    assertThat(r.getBody().getPickupOtp()).isNull();
                    assertThat(r.getBody().getDeliveredOtp()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void assignManual_orderNotFound_returns404() {
        when(deliveryService.assign(eq(TENANT_ID), eq(ORDER_ID), eq(AGENT_ID)))
                .thenReturn(Mono.error(new ProductNotFoundException(ORDER_ID)));

        StepVerifier.create(controller.assignManual(ORDER_ID, AGENT_ID, mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(404))
                .verifyComplete();
    }

    @Test
    void assignManual_alreadyAssigned_returns409() {
        when(deliveryService.assign(eq(TENANT_ID), eq(ORDER_ID), eq(AGENT_ID)))
                .thenReturn(Mono.error(new IllegalStateException("Order already has an active delivery")));

        StepVerifier.create(controller.assignManual(ORDER_ID, AGENT_ID, mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(409))
                .verifyComplete();
    }

    @Test
    void pickupOtp_success_returns200() {
        Delivery delivery = sampleDelivery();
        when(deliveryService.getByOrderId(ORDER_ID)).thenReturn(Mono.just(delivery));

        StepVerifier.create(controller.pickupOtp(ORDER_ID, mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody()).isEqualTo("123456");
                })
                .verifyComplete();
    }

    @Test
    void pickupOtp_notFound_returns404() {
        when(deliveryService.getByOrderId(ORDER_ID))
                .thenReturn(Mono.error(new ProductNotFoundException(ORDER_ID)));

        StepVerifier.create(controller.pickupOtp(ORDER_ID, mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(404))
                .verifyComplete();
    }

    @Test
    void availableAgents_returnsFlux() {
        Agent a1 = sampleAgent();
        Agent a2 = sampleAgent();
        a2.setId(UUID.randomUUID());
        when(deliveryService.availableAgents(TENANT_ID)).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(controller.availableAgents(mockJwt()))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void transition_success_returns200() {
        Delivery delivery = sampleDelivery();
        delivery.setStatus(Delivery.PICKED_UP);
        when(deliveryService.transition(eq(USER_ID), eq("PICKED_UP"), eq("123456")))
                .thenReturn(Mono.just(delivery));

        StepVerifier.create(controller.transition(new TransitionRequest("PICKED_UP", "123456"), mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().getStatus()).isEqualTo(Delivery.PICKED_UP);
                })
                .verifyComplete();
    }

    @Test
    void transition_invalidState_returns409() {
        when(deliveryService.transition(eq(USER_ID), eq("DELIVERED"), eq("654321")))
                .thenReturn(Mono.error(new IllegalStateException("Cannot transition")));

        StepVerifier.create(controller.transition(new TransitionRequest("DELIVERED", "654321"), mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(409))
                .verifyComplete();
    }

    @Test
    void trackingByOrder_found_returns200() {
        Delivery delivery = sampleDelivery();
        when(deliveryService.getByOrderId(ORDER_ID)).thenReturn(Mono.just(delivery));

        StepVerifier.create(controller.trackingByOrder(ORDER_ID))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().getOrderId()).isEqualTo(ORDER_ID);
                })
                .verifyComplete();
    }

    @Test
    void trackingByOrder_notFound_returns404() {
        when(deliveryService.getByOrderId(ORDER_ID))
                .thenReturn(Mono.error(new ProductNotFoundException(ORDER_ID)));

        StepVerifier.create(controller.trackingByOrder(ORDER_ID))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(404))
                .verifyComplete();
    }

    @Test
    void myDeliveries_returnsFlux() {
        Delivery d = sampleDelivery();
        when(deliveryService.deliveriesForAgent(USER_ID)).thenReturn(Flux.just(d));

        StepVerifier.create(controller.myDeliveries(mockJwt()))
                .expectNextCount(1)
                .verifyComplete();
    }
}
