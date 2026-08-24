package com.loqal.communication.notify;

import com.loqal.communication.entity.Notification;
import com.loqal.communication.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification fan-out (PRD §8.4): template rendering, persistence, provider
 * dispatch, and per-recipient cooldown (digest-buffer stand-in).
 */
@Service
public class NotificationService {

    /** Same recipient+template is suppressed within this window (seconds). */
    static final int COOLDOWN_SECONDS = 60;

    private final NotificationRepository notificationRepository;
    private final NotificationChannel emailChannel;
    private final Map<String, LocalDateTime> recentSends = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationChannel emailChannel) {
        this.notificationRepository = notificationRepository;
        this.emailChannel = emailChannel;
    }

    /**
     * Renders {@code bodyTemplate} replacing {{var}} placeholders, persists a
     * notification row and dispatches through the channel.
     */
    public Mono<Notification> send(UUID tenantId, String channel, String recipient,
                                   String template, String subject, String bodyTemplate,
                                   Map<String, String> variables) {
        String rendered = render(bodyTemplate, variables == null ? Map.of() : variables);
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTenantId(tenantId);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setTemplate(template);
        notification.setSubject(subject);
        notification.setBody(rendered);
        notification.setStatus(Notification.STATUS_PENDING);
        notification.setCreatedAt(LocalDateTime.now());

        return Mono.just(notification)
                .flatMap(n -> {
                    if (isCoolingDown(recipient, template)) {
                        n.setStatus(Notification.STATUS_RATE_LIMITED);
                        return notificationRepository.save(n);
                    }
                    markSent(recipient, template);
                    boolean ok = dispatch(n);
                    n.setStatus(ok ? Notification.STATUS_SENT : Notification.STATUS_FAILED);
                    if (!ok) {
                        n.setFailureReason("Provider rejected message");
                    } else {
                        n.setSentAt(LocalDateTime.now());
                    }
                    return notificationRepository.save(n);
                });
    }

    boolean dispatch(Notification notification) {
        try {
            if (Notification.CHANNEL_EMAIL.equals(notification.getChannel())) {
                return emailChannel.send(notification.getRecipient(),
                        notification.getSubject(), notification.getBody());
            }
            // SMS/PUSH channels land in Phase 3
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isCoolingDown(String recipient, String template) {
        LocalDateTime last = recentSends.get(key(recipient, template));
        return last != null && last.isAfter(LocalDateTime.now().minusSeconds(COOLDOWN_SECONDS));
    }

    void markSent(String recipient, String template) {
        recentSends.put(key(recipient, template), LocalDateTime.now());
    }

    private static String key(String recipient, String template) {
        return recipient + ":" + template;
    }

    /** Replaces {{name}} placeholders; unknown placeholders become empty strings. */
    static String render(String templateBody, Map<String, String> variables) {
        String out = templateBody;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return out.replaceAll("\\{\\{[^}]+}}", "");
    }
}
