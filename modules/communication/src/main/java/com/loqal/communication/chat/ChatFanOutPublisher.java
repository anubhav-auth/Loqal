package com.loqal.communication.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.communication.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Cross-instance fan-out (PRD Phase 3): every persisted chat frame is
 * published to Kafka so all app instances (including this one) broadcast it
 * to their locally-connected WebSocket sessions — exactly once per instance.
 */
@Service
@Slf4j
public class ChatFanOutPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatFanOutPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ChatMessage message) {
        try {
            kafkaTemplate.send("chat-messages", message.getRoomId(), objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Failed to publish chat frame for room {}: {}", message.getRoomId(), e.getMessage());
        }
    }
}
