package com.Loqal.orderservice.repository;

import com.Loqal.orderservice.entity.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface OrderRepository extends R2dbcRepository<Order, UUID> {
    Flux<Order> findAllByCustomerId(UUID customerId);
    Flux<Order> findAllByMerchantId(UUID merchantId);
    Mono<Order> findByCustomerIdAndId(UUID customerId, UUID orderId);
}
