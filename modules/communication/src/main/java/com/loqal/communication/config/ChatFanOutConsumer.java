package com.loqal.communication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.communication.chat.ChatWebSocketHandler;
import com.loqal.contracts.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Broadcasts chat frames received from Kafka to this instance's local
 * WebSocket sessions (multi-instance fan-in, PRD Phase 3).
 */
@Component
@Slf4j
public class ChatFanOutConsumer {

    private final ChatWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatFanOutConsumer(ChatWebSocketHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.CHAT_MESSAGES, groupId = "communication-chat")
    public void onChatMessage(String payload) {
        try {
            String roomId = objectMapper.readTree(payload).path("roomId").asText(null);
            if (roomId != null) {
                handler.broadcast(roomId, payload);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast chat frame: {}", e.getMessage());
        }
    }
}
