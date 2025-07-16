package com.loqal.paymentservice.controller;

import com.loqal.paymentservice.entity.dto.PaymentRequest;
import com.loqal.paymentservice.entity.dto.PaymentResponse;
import com.loqal.paymentservice.entity.dto.RefundRequest;
import com.loqal.paymentservice.entity.dto.RefundResponse;
import com.loqal.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @PostMapping("/{paymentId}/capture")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.capturePayment(paymentId));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<RefundResponse> initiateRefund(@PathVariable String paymentId, @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.initiateRefund(paymentId, request));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }
}