package com.Loqal.orderservice.dto.events;

import java.util.UUID;

public record RefundRequestedEvent(
        UUID orderId,
        UUID razorpayPaymentId,
        double amount
) {}