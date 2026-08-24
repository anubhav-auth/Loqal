package com.loqal.communication.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Real-time chat over WebSocket (PRD §8.4).
 *
 * Protocol (JSON text frames):
 *   client -> {"roomId":"order:{id}","senderId":"uuid","senderRole":"CUSTOMER","content":"hi"}
 *   server <- same shape, echoed to every session in the room after persistence.
 *
 * Presence is in-memory per instance; multi-instance fan-out goes through
 * Kafka in Phase 3.
 */
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Many<String> outbound = Sinks.many().unicast().onBackpressureBuffer();

        Mono<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .concatMap(payload -> handleFrame(session, payload, outbound))
                .onErrorResume(e -> {
                    log.warn("Chat frame error: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();

        return session.send(outbound.asFlux().map(session::textMessage))
                .then(inbound);
    }

    private Mono<Void> handleFrame(WebSocketSession session, String payload, Sinks.Many<String> outbound) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String roomId = node.path("roomId").asText(null);
            UUID senderId = UUID.fromString(node.path("senderId").asText(""));
            String senderRole = node.path("senderRole").asText("USER");
            String content = node.path("content").asText("");

            if (roomId == null || roomId.isBlank()) {
                return Mono.empty();
            }
            List<WebSocketSession> sessions = rooms.computeIfAbsent(roomId,
                    k -> new CopyOnWriteArrayList<>());
            if (!sessions.contains(session)) {
                sessions.add(session);
                // keep the room list from growing unbounded across reconnects
                sessions.removeIf(s -> !s.isOpen());
            }

            return chatService.save(null, roomId, senderId, senderRole, content)
                    .doOnNext(saved -> {
                        String frame = toJson(saved);
                        for (WebSocketSession s : sessions) {
                            if (s.isOpen()) {
                                outbound.tryEmitNext(frame);
                            }
                        }
                    })
                    .then();
        } catch (Exception e) {
            log.warn("Malformed chat frame: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private String toJson(com.loqal.communication.entity.ChatMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return "{}";
        }
    }
}
