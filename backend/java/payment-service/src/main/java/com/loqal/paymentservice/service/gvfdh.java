package com.loqal.paymentservice.service;


import com.loqal.paymentservice.entity.Payment;
import com.loqal.paymentservice.entity.dto.PaymentRequest;
import com.loqal.paymentservice.entity.dto.PaymentResponse;
import com.loqal.paymentservice.entity.dto.RefundRequest;
import com.loqal.paymentservice.entity.dto.RefundResponse;
import com.loqal.paymentservice.repository.PaymentRepository;
import com.loqal.paymentservice.repository.RefundRepository;
import com.loqal.paymentservice.util.TenantContextHolder;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RefundRepository refundRepository;
    @Autowired
    private RazorpayClient razorpayClient;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
//    @CircuitBreaker(name = "razorpay", fallbackMethod = "fallback")
//    @Retry(name = "razorpay")
    public PaymentResponse initiatePayment(PaymentRequest request) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount() * 100); // Razorpay uses paise
            orderRequest.put("currency", request.getCurrency());
            Order razorpayOrder = razorpayClient.Orders.create(orderRequest);

            Payment payment = new Payment();
            payment.setId(UUID.randomUUID());
            payment.setTenantId(UUID.fromString(TenantContextHolder.getTenantId()));
            payment.setOrderId(UUID.fromString(request.getOrderId()));
            payment.setUserId(UUID.fromString(getUserIdFromJwt()));
            payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus("INITIATED");
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentResponse response = new PaymentResponse();
            response.setPaymentId(payment.getId().toString());
            response.setOrderId(request.getOrderId());
            response.setAmount(request.getAmount());
            response.setStatus("INITIATED");
            response.setPaymentLink(razorpayOrder.get("short_url").toString());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate payment", e);
        }
    }

    @Transactional
    public PaymentResponse capturePayment(String paymentId) {
        // Implementation for capturing payment
        // Update payment status and publish Kafka event
        return new PaymentResponse();
    }

    @Transactional
    public RefundResponse initiateRefund(String paymentId, RefundRequest request) {
        // Implementation for initiating refund
        // Update refund status and publish Kafka event
        return new RefundResponse();
    }

    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (!payment.getTenantId().toString().equals(TenantContextHolder.getTenantId())) {
            throw new SecurityException("Unauthorized access to payment");
        }

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId().toString());
        response.setOrderId(payment.getOrderId().toString());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        return response;
    }

    private String getUserIdFromJwt() {
        // Extract user_id from JWT claims
        return "user_id_placeholder"; // Replace with actual JWT claim extraction
    }

    public PaymentResponse fallback(PaymentRequest request, Throwable t) {
        // Fallback logic for circuit breaker
        throw new RuntimeException("Payment service unavailable", t);
    }
}
