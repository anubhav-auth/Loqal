package com.loqal.catalog.repository;

import com.loqal.catalog.entity.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductRepository extends R2dbcRepository<Product, UUID> {

    @Query("SELECT * FROM products WHERE id = :id FOR UPDATE")
    Mono<Product> findByIdWithPessimisticLock(UUID id);

    Flux<Product> findAllByMerchantId(UUID merchantId);

    Flux<Product> findAllByNameIgnoreCase(String query);
}