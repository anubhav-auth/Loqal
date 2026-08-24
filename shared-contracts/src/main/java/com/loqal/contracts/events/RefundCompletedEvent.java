package com.loqal.contracts.events;

import java.util.UUID;

/**
 * Emitted by the payments module when a refund settles (PRD Appendix B.7).
 */
public record RefundCompletedEvent(
        UUID orderId,
        UUID refundId,
        String razorpayRefundId,
        long amountMinor,
        String status
) {}
