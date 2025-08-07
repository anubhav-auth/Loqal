package com.loqal.paymentservice.controller;

import com.loqal.paymentservice.entity.events.PaymentServiceRequest;
import com.loqal.paymentservice.entity.events.PaymentServiceResponse;
import com.loqal.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/order")
    public Mono<ResponseEntity<PaymentServiceResponse>> createOrder(@RequestBody PaymentServiceRequest request) {
        return paymentService.createRazorpayOrder(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/webhook/razorpay")
    public Mono<ResponseEntity<Void>> handleRazorpayWebhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        return paymentService.handleWebhook(payload, signature)
                .thenReturn(ResponseEntity.ok().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().<Void>build()));
    }
}