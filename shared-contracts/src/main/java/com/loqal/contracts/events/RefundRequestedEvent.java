package com.loqal.contracts.events;

import java.util.UUID;

/**
 * Money amounts use integer minor units (paise) per PRD §9.2 — never floating point.
 */
public record RefundRequestedEvent(
        UUID orderId,
        UUID razorpayPaymentId,
        long amountMinor
) {}
