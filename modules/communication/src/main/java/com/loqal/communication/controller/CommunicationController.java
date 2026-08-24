package com.loqal.communication.controller;

import com.loqal.communication.chat.ChatService;
import com.loqal.communication.entity.ChatMessage;
import com.loqal.communication.notify.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/communication")
@RequiredArgsConstructor
public class CommunicationController {

    private final NotificationService notificationService;
    private final ChatService chatService;

    public record SendNotificationRequest(UUID tenantId, String channel, String recipient,
                                          String template, String subject,
                                          String bodyTemplate, Map<String, String> variables) {}

    @PostMapping("/notifications/send")
    public Mono<ResponseEntity<com.loqal.communication.entity.Notification>> send(
            @RequestBody SendNotificationRequest request) {
        return notificationService.send(request.tenantId(), request.channel(), request.recipient(),
                        request.template(), request.subject(), request.bodyTemplate(),
                        request.variables() == null ? Map.of() : request.variables())
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    public record ChatMessageRequest(String roomId, String content) {}

    @PostMapping("/chat/messages")
    public Mono<ResponseEntity<ChatMessage>> postMessage(@RequestBody ChatMessageRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
        String role = jwt.getClaimAsStringList("roles") != null && !jwt.getClaimAsStringList("roles").isEmpty()
                ? jwt.getClaimAsStringList("roles").get(0) : "USER";
        return chatService.save(null, request.roomId(), userId, role, request.content())
                .map(message -> ResponseEntity.status(HttpStatus.CREATED).body(message))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @GetMapping("/chat/{roomId}/messages")
    public Flux<ChatMessage> history(@PathVariable String roomId) {
        return chatService.history(roomId);
    }
}
