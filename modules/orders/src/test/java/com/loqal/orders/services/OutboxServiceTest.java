package com.loqal.orders.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.contracts.events.OrderCancellationEvent;
import com.loqal.contracts.events.ProductOrderRequest;
import com.loqal.orders.entity.Order;
import com.loqal.orders.entity.OrderItem;
import com.loqal.orders.entity.OutboxEvent;
import com.loqal.orders.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxServiceTest {

    private OutboxEventRepository outboxEventRepository;
    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        objectMapper = new ObjectMapper();
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
        ReflectionTestUtils.setField(outboxService, "stockReservationRequestTopic", "order-creation-requested");
        ReflectionTestUtils.setField(outboxService, "stockReversionRequestTopic", "order-cancel");
    }

    @Test
    void requestStockReservation_savesCorrectOutboxEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        OrderItem item1 = new OrderItem();
        item1.setProductId(productId1);
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setProductId(productId2);
        item2.setQuantity(3);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(List.of(item1, item2));

        when(outboxEventRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        outboxService.requestStockReservation(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertEquals("Order", event.getAggregateType());
        assertEquals("STOCK_RESERVATION_REQUESTED", event.getEventType());
        assertEquals("order-creation-requested", event.getDestinationTopic());

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertEquals(orderId.toString(), payload.get("orderId").asText());
        assertEquals(2, payload.get("items").size());
        assertEquals(productId1.toString(), payload.get("items").get(0).get("productId").asText());
        assertEquals(2, payload.get("items").get(0).get("quantity").asInt());
        assertEquals(productId2.toString(), payload.get("items").get(1).get("productId").asText());
        assertEquals(3, payload.get("items").get(1).get("quantity").asInt());
    }

    @Test
    void requestStockReversion_savesCorrectOutboxEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCancellationEvent cancellationEvent = new OrderCancellationEvent(
                orderId, List.of(new ProductOrderRequest(productId, 5)));

        when(outboxEventRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        outboxService.requestStockReversion(cancellationEvent);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertEquals("Order", event.getAggregateType());
        assertEquals("STOCK_REVERSION_REQUESTED", event.getEventType());
        assertEquals("order-cancel", event.getDestinationTopic());

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertEquals(orderId.toString(), payload.get("orderId").asText());
        assertEquals(1, payload.get("items").size());
        assertEquals(productId.toString(), payload.get("items").get(0).get("productId").asText());
        assertEquals(5, payload.get("items").get(0).get("quantity").asInt());
    }

    @Test
    void requestStockReservation_setsCorrectTopic() {
        UUID orderId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(List.of(item));

        when(outboxEventRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        outboxService.requestStockReservation(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals("order-creation-requested", captor.getValue().getDestinationTopic());
    }

    @Test
    void requestStockReversion_setsCorrectTopic() {
        OrderCancellationEvent event = new OrderCancellationEvent(
                UUID.randomUUID(), List.of(new ProductOrderRequest(UUID.randomUUID(), 1)));

        when(outboxEventRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        outboxService.requestStockReversion(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals("order-cancel", captor.getValue().getDestinationTopic());
    }
}
