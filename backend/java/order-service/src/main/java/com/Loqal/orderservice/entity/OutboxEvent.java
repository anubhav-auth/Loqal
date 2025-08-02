package com.Loqal.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "Order"

    @Column(nullable = false)
    private String eventType; // e.g., "ORDER_CANCELLATION_REQUESTED"

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON payload

    @Column(nullable = false)
    private String destinationTopic; // Kafka topic name

    private LocalDateTime createdAt = LocalDateTime.now();
}