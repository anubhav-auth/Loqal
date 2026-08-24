package com.loqal.communication.notify;

/**
 * Delivery channel provider (PRD §8.4). Implementations: logging stub now,
 * SMTP/SMS gateways later — the interface isolates blocking SDKs.
 */
public interface NotificationChannel {

    /** @return true if the message was dispatched successfully. */
    boolean send(String recipient, String subject, String body);
}
