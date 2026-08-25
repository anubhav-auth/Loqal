package com.loqal.communication.controller;

import com.loqal.communication.chat.ChatService;
import com.loqal.communication.controller.CommunicationController.ChatMessageRequest;
import com.loqal.communication.controller.CommunicationController.SendNotificationRequest;
import com.loqal.communication.entity.ChatMessage;
import com.loqal.communication.entity.Notification;
import com.loqal.communication.notify.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunicationControllerTest {

    private NotificationService notificationService;
    private ChatService chatService;
    private CommunicationController controller;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        chatService = mock(ChatService.class);
        controller = new CommunicationController(notificationService, chatService);
    }

    @Test
    void sendNotification_success() {
        UUID tenantId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTenantId(tenantId);
        notification.setChannel("EMAIL");
        notification.setRecipient("user@test.com");
        notification.setTemplate("order-confirmed");
        notification.setSubject("Order Confirmed");
        notification.setBody("Your order is confirmed");
        notification.setStatus(Notification.STATUS_SENT);
        notification.setCreatedAt(LocalDateTime.now());

        when(notificationService.send(eq(tenantId), eq("EMAIL"), eq("user@test.com"),
                eq("order-confirmed"), eq("Order Confirmed"), eq("Your order is confirmed"),
                eq(Map.of()))).thenReturn(Mono.just(notification));

        SendNotificationRequest request = new SendNotificationRequest(
                tenantId, "EMAIL", "user@test.com", "order-confirmed",
                "Order Confirmed", "Your order is confirmed", Map.of());

        StepVerifier.create(controller.send(request))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals(notification, resp.getBody());
                })
                .verifyComplete();
    }

    @Test
    void sendNotification_failure() {
        UUID tenantId = UUID.randomUUID();
        when(notificationService.send(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("channel down")));

        SendNotificationRequest request = new SendNotificationRequest(
                tenantId, "EMAIL", "user@test.com", "order-confirmed",
                "Order Confirmed", "Your order is confirmed", null);

        StepVerifier.create(controller.send(request))
                .assertNext(resp -> assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void postMessage_success() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("user_id", userId.toString())
                .claim("roles", List.of("ROLE_USER"))
                .build();

        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setRoomId("room-1");
        message.setSenderId(userId);
        message.setSenderRole("ROLE_USER");
        message.setContent("Hello!");
        message.setCreatedAt(LocalDateTime.now());

        when(chatService.save(isNull(), eq("room-1"), eq(userId), eq("ROLE_USER"), eq("Hello!")))
                .thenReturn(Mono.just(message));

        ChatMessageRequest request = new ChatMessageRequest("room-1", "Hello!");

        StepVerifier.create(controller.postMessage(request, jwt))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CREATED, resp.getStatusCode());
                    assertEquals(message, resp.getBody());
                })
                .verifyComplete();
    }

    @Test
    void postMessage_invalidContent() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("user_id", userId.toString())
                .claim("roles", List.of("ROLE_USER"))
                .build();

        when(chatService.save(any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid message content")));

        ChatMessageRequest request = new ChatMessageRequest("room-1", "");

        StepVerifier.create(controller.postMessage(request, jwt))
                .assertNext(resp -> assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void historyReturnsMessages() {
        ChatMessage msg1 = new ChatMessage();
        msg1.setId(UUID.randomUUID());
        msg1.setRoomId("room-1");
        msg1.setContent("first");
        ChatMessage msg2 = new ChatMessage();
        msg2.setId(UUID.randomUUID());
        msg2.setRoomId("room-1");
        msg2.setContent("second");

        when(chatService.history("room-1")).thenReturn(Flux.just(msg1, msg2));

        StepVerifier.create(controller.history("room-1"))
                .assertNext(m -> assertEquals("first", m.getContent()))
                .assertNext(m -> assertEquals("second", m.getContent()))
                .verifyComplete();
    }
}
