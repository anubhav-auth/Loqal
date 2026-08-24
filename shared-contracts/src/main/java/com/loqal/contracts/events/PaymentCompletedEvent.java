package com.loqal.contracts.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Provider references (razorpayOrderId / razorpayPaymentId) are Strings,
 * matching Razorpay's "order_..." / "pay_..." id formats.
 */
public record PaymentCompletedEvent(
        UUID orderId,
        String razorpayOrderId,
        String razorpayPaymentId,
        String status,
        LocalDateTime paidAt
) {}
