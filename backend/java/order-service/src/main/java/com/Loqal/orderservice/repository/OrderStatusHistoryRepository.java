package com.Loqal.orderservice.repository;

import com.Loqal.orderservice.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
}