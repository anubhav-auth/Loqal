package com.loqal.communication.chat;

import com.loqal.communication.entity.ChatMessage;
import com.loqal.communication.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chat persistence + history (PRD §8.4). Rooms are scoped strings, e.g.
 * "order:{orderId}" for customer↔dispatcher↔agent conversations.
 */
@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public Mono<ChatMessage> save(UUID roomIdSeedIgnored, String roomId, UUID senderId,
                                  String senderRole, String content) {
        if (roomId == null || roomId.isBlank()) {
            return Mono.error(new IllegalArgumentException("roomId is required"));
        }
        if (content == null || content.isBlank() || content.length() > 4000) {
            return Mono.error(new IllegalArgumentException("Invalid message content"));
        }
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setRoomId(roomId);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return chatMessageRepository.save(message);
    }

    /** Newest-first storage; returned oldest-first for display. */
    public Flux<ChatMessage> history(String roomId) {
        return chatMessageRepository.findTop100ByRoomIdOrderByCreatedAtDesc(roomId)
                .collectList()
                .flatMapMany(list -> Flux.fromIterable(list.reversed()));
    }
}
