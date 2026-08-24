package com.loqal.communication.repository;

import com.loqal.communication.entity.ChatMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ChatMessageRepository extends R2dbcRepository<ChatMessage, UUID> {

    Flux<ChatMessage> findTop100ByRoomIdOrderByCreatedAtDesc(String roomId);
}
