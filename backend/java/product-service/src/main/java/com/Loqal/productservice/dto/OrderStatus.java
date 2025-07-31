package com.Loqal.productservice.dto;


import lombok.AllArgsConstructor;

public enum OrderStatus {
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_REJECTED,
    ORDER_DISPATCHED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_RETURNED,
    ORDER_PENDING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    DELIVERY_ASSIGNED,
    DELIVERY_COMPLETED,
    DELIVERY_FAILED
}
