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
@Table("notifications")
public class Notification {

    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_PUSH = "PUSH";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RATE_LIMITED = "RATE_LIMITED";

    @Id
    private UUID id;
    private UUID tenantId;
    private String channel;
    private String recipient;
    private String template;
    private String subject;
    private String body;
    private String status;
    private String failureReason;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
