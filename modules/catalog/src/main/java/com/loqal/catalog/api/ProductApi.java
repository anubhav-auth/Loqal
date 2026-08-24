package com.loqal.catalog.api;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Published API of the catalog module. Other modules must depend only on this
 * interface — never on internal services, repositories or entities.
 */
public interface ProductApi {

    /** Price snapshot for an active product; errors PRODUCT_NOT_FOUND if missing. */
    Mono<ProductPrice> findPrice(UUID productId);

    record ProductPrice(UUID productId, long priceMinor, int quantityAvailable, boolean active) {
    }
}
