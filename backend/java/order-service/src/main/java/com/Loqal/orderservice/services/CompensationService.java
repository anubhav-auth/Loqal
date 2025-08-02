package com.Loqal.orderservice.services;

import com.Loqal.orderservice.dto.ProductOrderRequest;
import com.Loqal.orderservice.entity.OutboxEvent;
import com.Loqal.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompensationService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.order-cancel}")
    private String orderCancellationTopic;

    @SneakyThrows // Simplifies exception handling for json processing
    @Transactional(propagation = Propagation.REQUIRES_NEW) // <-- CRITICAL: Runs in a new transaction
    public void scheduleStockReversion(List<ProductOrderRequest> itemsToRevert) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setEventType("STOCK_REVERSION_REQUESTED");
        event.setDestinationTopic(orderCancellationTopic);
        event.setPayload(objectMapper.writeValueAsString(itemsToRevert));
        outboxEventRepository.save(event);
    }
}