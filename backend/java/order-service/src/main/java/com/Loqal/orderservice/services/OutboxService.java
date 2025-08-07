package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.ProductOrderRequest;
import com.Loqal.orderservice.dto.events.OrderCancellationEvent;
import com.Loqal.orderservice.dto.events.StockReservationRequest;
import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.entity.OutboxEvent;
import com.Loqal.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.order-creation-requested}")
    private String stockReservationRequestTopic;

    @Value("${spring.kafka.topic.order-cancel}")
    private String stockReversionRequestTopic;

    @SneakyThrows
    @Transactional
    public void requestStockReservation(Order order) {
        List<ProductOrderRequest> items = order.getItems().stream()
                .map(item -> new ProductOrderRequest(item.getProductId(), item.getPriceAtPurchase(), item.getQuantity()))
                .collect(Collectors.toList());

        StockReservationRequest payload = new StockReservationRequest(order.getId(), items);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setEventType("STOCK_RESERVATION_REQUESTED");
        event.setDestinationTopic(stockReservationRequestTopic);
        event.setPayload(objectMapper.writeValueAsString(payload));
        outboxEventRepository.save(event);
    }

    @SneakyThrows
    @Transactional
    public void requestStockReversion(OrderCancellationEvent cancellationEvent) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setEventType("STOCK_REVERSION_REQUESTED");
        event.setDestinationTopic(stockReversionRequestTopic);
        event.setPayload(objectMapper.writeValueAsString(cancellationEvent));
        outboxEventRepository.save(event);
    }
}