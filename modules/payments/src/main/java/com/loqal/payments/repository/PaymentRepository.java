package com.loqal.payments.repository;

import com.loqal.payments.entity.Payment;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository extends R2dbcRepository<Payment, UUID> {

    Mono<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Mono<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Mono<Boolean> existsByRazorpayPaymentId(String razorpayPaymentId);
}
