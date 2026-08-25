package com.loqal.catalog.controller;

import com.loqal.catalog.entity.Category;
import com.loqal.catalog.entity.Product;
import com.loqal.catalog.entity.ProductDTO;
import com.loqal.catalog.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productService);
    }

    private Product sampleProduct(UUID id) {
        Product p = new Product();
        p.setId(id);
        p.setName("Test Product");
        p.setDescription("A test product");
        p.setCategory_name("Food");
        p.setCategory_description("Edible items");
        p.setPriceMinor(10000);
        p.setQuantity(50);
        p.setImage_urls(List.of("img1.jpg"));
        p.setMerchantId(UUID.randomUUID());
        return p;
    }

    @Test
    void createProduct_success_returns201() {
        UUID merchantId = UUID.randomUUID();
        Product product = sampleProduct(UUID.randomUUID());
        ProductDTO dto = new ProductDTO(null, "Test Product", "A test product",
                new Category("Food", "Edible items"), 10000, 50, List.of("img1.jpg"));

        when(productService.create(any(ProductDTO.class), eq(merchantId)))
                .thenReturn(Mono.just(product));

        StepVerifier.create(controller.createProduct(merchantId, dto))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().value()).isEqualTo(201);
                    assertThat(r.getBody().getName()).isEqualTo("Test Product");
                })
                .verifyComplete();
    }

    @Test
    void getProductById_found_returns200() {
        UUID id = UUID.randomUUID();
        Product product = sampleProduct(id);
        when(productService.getById(id)).thenReturn(Mono.just(product));

        StepVerifier.create(controller.getProductById(id))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().getId()).isEqualTo(id);
                })
                .verifyComplete();
    }

    @Test
    void getProductById_notFound_returns404() {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getProductById(id))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(404))
                .verifyComplete();
    }

    @Test
    void searchProducts_withQuery_returnsProducts() {
        Product p1 = sampleProduct(UUID.randomUUID());
        Product p2 = sampleProduct(UUID.randomUUID());
        when(productService.search("momo")).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(controller.searchProducts("momo"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void searchProducts_blankQuery_returnsEmpty() {
        when(productService.search("")).thenReturn(Flux.empty());

        StepVerifier.create(controller.searchProducts(""))
                .verifyComplete();
    }

    @Test
    void deleteProduct_success_returns204() {
        UUID id = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        when(productService.delete(id, merchantId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteProduct(id, merchantId))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(204))
                .verifyComplete();
    }
}
