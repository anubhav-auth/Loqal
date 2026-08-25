package com.loqal.communication.config;

import com.loqal.communication.chat.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ChatFanOutConsumerTest {

    private ChatWebSocketHandler handler;
    private ChatFanOutConsumer consumer;

    @BeforeEach
    void setUp() {
        handler = mock(ChatWebSocketHandler.class);
        consumer = new ChatFanOutConsumer(handler);
    }

    @Test
    void onChatMessage_validRoomId_callsBroadcast() {
        String payload = "{\"roomId\":\"room-abc\",\"senderId\":\"550e8400-e29b-41d4-a716-446655440000\",\"content\":\"Hi\"}";

        consumer.onChatMessage(payload);

        verify(handler).broadcast("room-abc", payload);
    }

    @Test
    void onChatMessage_missingRoomId_doesNotCallBroadcast() {
        String payload = "{\"senderId\":\"550e8400-e29b-41d4-a716-446655440000\",\"content\":\"Hi\"}";

        consumer.onChatMessage(payload);

        verify(handler, never()).broadcast(any(), any());
    }

    @Test
    void onChatMessage_malformedJson_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onChatMessage("not json {{{"));
        verify(handler, never()).broadcast(any(), any());
    }

    @Test
    void onChatMessage_emptyJson_doesNotCallBroadcast() {
        consumer.onChatMessage("{}");
        verify(handler, never()).broadcast(any(), any());
    }

    @Test
    void onChatMessage_nullRoomIdInJson_doesNotCallBroadcast() {
        String payload = "{\"roomId\":null,\"content\":\"test\"}";
        consumer.onChatMessage(payload);
        verify(handler, never()).broadcast(any(), any());
    }
}
