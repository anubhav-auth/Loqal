package com.loqal.paymentservice.entity.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID orderId,
        String razorpayOrderId,
        String razorpayPaymentId,
        String status,
        LocalDateTime paidAt
) {}