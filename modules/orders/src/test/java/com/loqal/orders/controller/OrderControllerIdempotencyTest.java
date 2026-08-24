package com.loqal.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.orders.dto.OrderRequest;
import com.loqal.orders.dto.events.OrderCreationResponse;
import com.loqal.orders.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

class OrderControllerIdempotencyTest {

    private OrderService orderService;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOps;
    private OrderController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderService = Mockito.mock(OrderService.class);
        redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        valueOps = Mockito.mock(ReactiveValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        controller = new OrderController(orderService, redisTemplate, new ObjectMapper());
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("user_id", UUID.randomUUID().toString())
                .build();
    }

    private OrderRequest request() {
        OrderRequest req = new OrderRequest();
        req.setItems(List.of());
        return req;
    }

    @Test
    void replaysCachedResponseOnIdempotencyHit() throws Exception {
        String key = "test-key";
        OrderCreationResponse cached = new OrderCreationResponse(UUID.randomUUID(), "order_123");
        String cachedJson = new ObjectMapper().writeValueAsString(cached);

        Mockito.when(valueOps.get("idempotency:" + key)).thenReturn(Mono.just(cachedJson));

        StepVerifier.create(controller.createOrder(request(), jwt(), key))
                .assertNext(response -> {
                    org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
                    org.junit.jupiter.api.Assertions.assertEquals(cached.orderId(),
                            response.getBody().orderId());
                })
                .verifyComplete();

        Mockito.verifyNoInteractions(orderService);
    }

    @Test
    void createsOrderAndCachesWhenKeyMiss() {
        String key = "new-key";
        Mockito.when(valueOps.get("idempotency:" + key)).thenReturn(Mono.empty());

        OrderCreationResponse created = new OrderCreationResponse(UUID.randomUUID(), "order_456");
        Mockito.when(orderService.createOrder(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(created));
        Mockito.when(valueOps.set(Mockito.eq("idempotency:" + key), Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(controller.createOrder(request(), jwt(), key))
                .assertNext(response -> {
                    org.junit.jupiter.api.Assertions.assertEquals(201, response.getStatusCode().value());
                    org.junit.jupiter.api.Assertions.assertEquals(created.razorpayOrderId(),
                            response.getBody().razorpayOrderId());
                })
                .verifyComplete();

        Mockito.verify(valueOps).set(Mockito.eq("idempotency:" + key), Mockito.anyString(), Mockito.any());
    }
}
