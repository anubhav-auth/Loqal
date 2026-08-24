package com.loqal.orders.dto;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {}
