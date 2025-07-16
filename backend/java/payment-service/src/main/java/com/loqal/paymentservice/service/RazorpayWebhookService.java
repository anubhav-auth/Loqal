package com.loqal.paymentservice.service;

import com.loqal.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RazorpayWebhookService {
    @Autowired
    private PaymentRepository paymentRepository;

    public void handleWebhook(String payload) {
        // Validate webhook signature and process events
        // Update payment status and publish Kafka events
    }
}