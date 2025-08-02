package com.Loqal.orderservice.repository;

import com.Loqal.orderservice.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findById(UUID id); // Override findById to lock it

    List<Order> findAllByCustomerId(UUID customerId);

    List<Order> findAllByMerchantId(UUID customerId);

    Optional<Order> findAllByCustomerIdAndId(UUID customerId, UUID orderId);
}