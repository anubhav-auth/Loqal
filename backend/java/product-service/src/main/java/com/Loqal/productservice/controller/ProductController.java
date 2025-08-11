package com.Loqal.productservice.controller;

import com.Loqal.productservice.dto.UpdateStockRequestDto;
import com.Loqal.productservice.entity.Category;
import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.query.Update;
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

    @PostMapping("/{merchantId}")
    public Mono<ResponseEntity<Product>> createProduct(@PathVariable UUID merchantId, @RequestBody ProductDTO product) {
        return productService.create(product, merchantId)
                .map(createdProduct -> new ResponseEntity<>(createdProduct, HttpStatus.CREATED));
    }

    @GetMapping("/merchant")
    public Flux<ProductDTO> getProductsForMerchant(UUID merchantId) {
        return productService.getAllByMerchant(merchantId)
                .map(item -> new ProductDTO(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        new Category(item.getCategory_name(), item.getCategory_description()),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getImage_urls()
                ));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getProductById(@PathVariable UUID id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{productId}/{merchantId}")
    public Mono<ResponseEntity<ProductDTO>> updateProduct(@PathVariable UUID productId, @PathVariable UUID merchantId, @RequestBody UpdateStockRequestDto product) {
        return productService.update(productId, merchantId, product)
                .map(item -> new ProductDTO(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        new Category(item.getCategory_name(), item.getCategory_description()),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getImage_urls()
                ))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/{merchantId}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable UUID id, @PathVariable UUID merchantId) {
        return productService.delete(id, merchantId)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/search")
    public Flux<Product> searchProducts(@RequestParam String query) {
        return productService.search(query);
    }
}