package com.loqal.paymentservice.repository;



import com.loqal.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Payment findByRazorpayPaymentIdAndTenantId(String razorpayPaymentId, UUID tenantId);
}