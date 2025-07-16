package com.loqal.paymentservice.controller;

import com.loqal.paymentservice.service.RazorpayWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {
    @Autowired
    private RazorpayWebhookService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(@RequestBody String payload) {
        webhookService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}