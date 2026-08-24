package com.loqal.payments.repository;

import com.loqal.payments.entity.Refund;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface RefundRepository extends R2dbcRepository<Refund, UUID> {
}
