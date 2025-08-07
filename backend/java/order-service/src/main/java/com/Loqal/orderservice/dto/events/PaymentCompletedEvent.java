package com.Loqal.orderservice.dto.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID orderId,
        UUID razorpayOrderId,
        UUID razorpayPaymentId,
        String status,
        LocalDateTime paidAt
) {}
