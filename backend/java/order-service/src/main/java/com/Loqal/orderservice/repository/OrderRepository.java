package com.Loqal.orderservice.repository;

import com.Loqal.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<List<Order>> findAllByCustomerId(UUID customerId);
    Optional<List<Order>> findAllByMerchantId(UUID customerId);
    Optional<List<Order>> findAllByCustomerIdAndId(UUID customerId, UUID orderId);
}