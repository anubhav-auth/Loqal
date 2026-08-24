package com.loqal.communication.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_messages")
public class ChatMessage {
    @Id
    private UUID id;
    private String roomId;
    private UUID senderId;
    private String senderRole;
    private String content;
    private LocalDateTime createdAt;
}
