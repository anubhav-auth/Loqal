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

@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final ChatFanOutPublisher fanOutPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<RegisteredSession>> rooms = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ChatFanOutPublisher fanOutPublisher) {
        this.chatService = chatService;
        this.fanOutPublisher = fanOutPublisher;
    }

    private record RegisteredSession(WebSocketSession session, Sinks.Many<String> sink) {}

    /** Called by the Kafka consumer for every frame on any instance. */
    public void broadcast(String roomId, String frame) {
        List<RegisteredSession> sessions = rooms.get(roomId);
        if (sessions == null) return;
        sessions.removeIf(rs -> !rs.session().isOpen());
        for (RegisteredSession rs : sessions) {
            rs.sink().tryEmitNext(frame);
        }
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
            List<RegisteredSession> sessions = rooms.computeIfAbsent(roomId,
                    k -> new CopyOnWriteArrayList<>());
            RegisteredSession registered = new RegisteredSession(session, outbound);
            if (!sessions.contains(registered)) {
                sessions.add(registered);
                // keep the room list from growing unbounded across reconnects
                sessions.removeIf(rs -> !rs.session().isOpen());
            }

            return chatService.save(null, roomId, senderId, senderRole, content)
                    .doOnNext(fanOutPublisher::publish)
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
