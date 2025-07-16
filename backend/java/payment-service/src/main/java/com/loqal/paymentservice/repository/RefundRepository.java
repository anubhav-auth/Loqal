package com.loqal.paymentservice.repository;

import com.loqal.paymentservice.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
}