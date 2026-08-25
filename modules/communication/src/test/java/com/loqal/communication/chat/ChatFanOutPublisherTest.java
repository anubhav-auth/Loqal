package com.loqal.communication.chat;

import com.loqal.communication.entity.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatFanOutPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ChatFanOutPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        publisher = new ChatFanOutPublisher(kafkaTemplate);
    }

    private ChatMessage buildMessage(String roomId, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setRoomId(roomId);
        message.setSenderId(UUID.randomUUID());
        message.setSenderRole("USER");
        message.setContent(content);
        return message;
    }

    @Test
    void publish_sendsMessageToKafkaWithRoomIdAsKey() {
        ChatMessage message = buildMessage("room-123", "Hello");

        publisher.publish(message);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("chat-messages"), keyCaptor.capture(), payloadCaptor.capture());

        assertEquals("room-123", keyCaptor.getValue());
        assertTrue(payloadCaptor.getValue().contains("room-123"));
        assertTrue(payloadCaptor.getValue().contains("Hello"));
    }

    @Test
    void publish_handlesNullSenderId() {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setRoomId("room-456");
        message.setSenderId(null);
        message.setSenderRole("MERCHANT");
        message.setContent("Thanks");

        publisher.publish(message);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("chat-messages"), any(String.class), payloadCaptor.capture());
        assertTrue(payloadCaptor.getValue().contains("MERCHANT"));
    }

    @Test
    void publish_doesNotThrowOnKafkaFailure() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenThrow(new RuntimeException("Kafka down"));

        ChatMessage message = buildMessage("room-789", "test");

        assertDoesNotThrow(() -> publisher.publish(message));
    }
}
