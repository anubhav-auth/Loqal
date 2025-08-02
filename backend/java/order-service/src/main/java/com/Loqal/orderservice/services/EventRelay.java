package com.Loqal.orderservice.services;

import com.Loqal.orderservice.entity.OutboxEvent;
import com.Loqal.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate; // Publish payload as String

    @Scheduled(fixedDelayString = "${outbox.relay.delay:10000}") // Run every 10 seconds
    @Transactional
    public void relayEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByOrderByCreatedAt();
        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} events in outbox to relay.", events.size());
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getDestinationTopic(), event.getPayload()).get(); // .get() makes it synchronous within the try
                outboxEventRepository.delete(event);
            } catch (Exception e) {
                log.error("Failed to publish event {} from outbox. It will be retried later. Error: {}", event.getId(), e.getMessage());
            }
        }
    }
}