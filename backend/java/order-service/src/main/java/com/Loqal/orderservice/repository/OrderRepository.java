package com.Loqal.orderservice.repository;

import com.Loqal.orderservice.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    List<Order> findAllByCustomerId(UUID customerId);

    List<Order> findAllByMerchantId(UUID customerId);

    Optional<Order> findAllByCustomerIdAndId(UUID customerId, UUID orderId);
}