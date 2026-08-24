package com.loqal.orders.dto.events;

import java.util.UUID;

public record OrderCreationResponse(UUID orderId, String razorpayOrderId) {}