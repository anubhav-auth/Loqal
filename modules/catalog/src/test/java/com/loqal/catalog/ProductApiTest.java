package com.loqal.catalog;

import com.loqal.catalog.api.ProductApi;
import com.loqal.catalog.entity.Product;
import com.loqal.catalog.repository.ProductRepository;
import com.loqal.catalog.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductApiTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductService(productRepository, mock(com.loqal.catalog.repository.ProcessedEventRepository.class), mock(org.springframework.kafka.core.KafkaTemplate.class));
    }

    @Test
    void findPriceConvertsToMinorUnits() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setPrice(19.99);
        product.setQuantity(5);
        when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));

        StepVerifier.create(productService.findPrice(product.getId()))
                .assertNext(price -> {
                    assertEquals(1999L, price.priceMinor());
                    assertEquals(5, price.quantityAvailable());
                })
                .verifyComplete();
    }

    @Test
    void findPriceErrorsForUnknownProduct() {
        when(productRepository.findById(org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(Mono.empty());
        StepVerifier.create(productService.findPrice(UUID.randomUUID()))
                .expectError(com.loqal.catalog.exception.ProductNotFoundException.class)
                .verify();
    }
}
