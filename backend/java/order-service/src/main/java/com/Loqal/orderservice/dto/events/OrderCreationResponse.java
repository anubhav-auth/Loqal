package com.Loqal.orderservice.dto.events;

import java.util.UUID;

public record OrderCreationResponse(UUID orderId, UUID razorpayOrderId) {}