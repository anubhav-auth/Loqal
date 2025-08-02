package com.Loqal.orderservice.dto;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {}
