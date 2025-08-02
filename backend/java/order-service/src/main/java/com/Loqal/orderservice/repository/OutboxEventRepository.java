package com.Loqal.orderservice.repository;

import com.Loqal.orderservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // Find events that need to be published
    List<OutboxEvent> findTop100ByOrderByCreatedAt();
}