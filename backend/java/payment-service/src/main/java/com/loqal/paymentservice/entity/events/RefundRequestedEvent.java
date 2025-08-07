package com.loqal.paymentservice.entity.events;

import java.util.UUID;

public record RefundRequestedEvent(
        UUID orderId,
        String razorpayPaymentId,
        double amount
) {}