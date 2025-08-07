package com.loqal.paymentservice.entity.events;

public record PaymentServiceRequest(String receipt, double amount) {}