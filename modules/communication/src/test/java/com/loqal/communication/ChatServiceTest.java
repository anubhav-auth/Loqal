package com.loqal.communication;

import com.loqal.communication.chat.ChatService;
import com.loqal.communication.entity.ChatMessage;
import com.loqal.communication.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

class ChatServiceTest {

    private ChatMessageRepository repository;
    private ChatService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = Mockito.mock(ChatMessageRepository.class);
        when(repository.save(Mockito.any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        service = new ChatService(repository);
    }

    @Test
    void rejectsBlankRoom() {
        StepVerifier.create(service.save(null, " ", UUID.randomUUID(), "USER", "hello"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void rejectsBlankOrOversizedContent() {
        UUID sender = UUID.randomUUID();
        StepVerifier.create(service.save(null, "room", sender, "USER", ""))
                .expectError(IllegalArgumentException.class)
                .verify();
        StepVerifier.create(service.save(null, "room", sender, "USER", "x".repeat(4001)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void historyReturnsOldestFirst() {
        ChatMessage newest = new ChatMessage();
        newest.setContent("newest");
        ChatMessage oldest = new ChatMessage();
        oldest.setContent("oldest");
        // repo returns newest-first (top-100 desc)
        Mockito.when(repository.findTop100ByRoomIdOrderByCreatedAtDesc("room"))
                .thenReturn(Flux.just(newest, oldest));

        StepVerifier.create(service.history("room"))
                .assertNext(m -> org.junit.jupiter.api.Assertions.assertEquals("oldest", m.getContent()))
                .assertNext(m -> org.junit.jupiter.api.Assertions.assertEquals("newest", m.getContent()))
                .verifyComplete();
    }
}
