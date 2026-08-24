package com.loqal.communication.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logging-backed email channel. Swap for SMTP/Nodemailer-style providers in
 * Phase 3 without touching callers (same seam as RazorpayGateway).
 */
@Component
@Slf4j
public class LoggingEmailChannel implements NotificationChannel {

    @Override
    public boolean send(String recipient, String subject, String body) {
        log.info("EMAIL to={} subject='{}' bodyLen={}", recipient, subject, body == null ? 0 : body.length());
        return true;
    }
}
