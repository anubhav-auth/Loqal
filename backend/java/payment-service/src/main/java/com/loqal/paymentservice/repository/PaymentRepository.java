package com.loqal.paymentservice.repository;

import com.loqal.paymentservice.entity.Payment;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface PaymentRepository extends R2dbcRepository<Payment, UUID> {
    Mono<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
}