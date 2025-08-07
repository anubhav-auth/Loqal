package com.Loqal.orderservice.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("outbox_events")
@Getter
@Setter
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateType;
    private String eventType;
    private String payload;
    private String destinationTopic;
    private LocalDateTime createdAt = LocalDateTime.now();
}
