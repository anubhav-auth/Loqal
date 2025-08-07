package com.Loqal.productservice.controller;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Mono<ResponseEntity<Product>> createProduct(@RequestBody ProductDTO product, @AuthenticationPrincipal Mono<Jwt> jwtMono) {
        return jwtMono
                .map(jwt -> UUID.fromString(jwt.getClaimAsString("tenant_id")))
                .flatMap(merchantId -> productService.create(product, merchantId))
                .map(createdProduct -> new ResponseEntity<>(createdProduct, HttpStatus.CREATED));
    }

    @GetMapping("/merchant")
    public Flux<Product> getProductsForMerchant(@AuthenticationPrincipal Mono<Jwt> jwtMono) {
        return jwtMono
                .map(jwt -> UUID.fromString(jwt.getClaimAsString("tenant_id")))
                .flatMapMany(productService::getAllByMerchant);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getProductById(@PathVariable UUID id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> updateProduct(@PathVariable UUID id, @RequestBody ProductDTO product, @AuthenticationPrincipal Mono<Jwt> jwtMono) {
        return jwtMono
                .map(jwt -> UUID.fromString(jwt.getClaimAsString("tenant_id")))
                .flatMap(merchantId -> productService.update(id, product, merchantId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable UUID id, @AuthenticationPrincipal Mono<Jwt> jwtMono) {
        return jwtMono
                .map(jwt -> UUID.fromString(jwt.getClaimAsString("tenant_id")))
                .flatMap(merchantId -> productService.delete(id, merchantId))
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/search")
    public Flux<Product> searchProducts(@RequestParam String query) {
        return productService.search(query);
    }
}