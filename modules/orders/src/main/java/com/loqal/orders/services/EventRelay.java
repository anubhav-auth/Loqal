package com.loqal.orders.services;

import com.loqal.orders.entity.OutboxEvent;
import com.loqal.orders.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionalOperator transactionalOperator;

    @Scheduled(fixedDelayString = "${outbox.relay.delay:10000}")
    public void relayEvents() {
        outboxEventRepository.findTop100ByOrderByCreatedAt()

                .concatMap(this::sendAndDeleteEvent)
                .doOnSubscribe(s -> log.debug("Starting outbox relay process..."))
                .doOnComplete(() -> log.debug("Outbox relay process finished for this cycle."))
                .subscribe();
    }

    private Mono<Void> sendAndDeleteEvent(OutboxEvent event) {
        return Mono.fromFuture(kafkaTemplate.send(event.getDestinationTopic(), event.getPayload()))
                .then(outboxEventRepository.delete(event))
                .as(transactionalOperator::transactional)
                .doOnSuccess(v -> log.info("Successfully relayed and deleted event {}", event.getId()))
                .onErrorResume(e -> {

                    log.error("Failed to relay event {} from outbox. It will be retried. Error: {}", event.getId(), e.getMessage());
                    return Mono.empty();
                });
    }
}
