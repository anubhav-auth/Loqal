package com.Loqal.productservice.controller;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.exception.InsufficientStockException;
import com.Loqal.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products") // REFACTORED: Using a base path for clarity.
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // =================================================================
    // == MERCHANT/USER FACING ENDPOINTS
    // =================================================================

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductDTO product, @AuthenticationPrincipal Jwt jwt) {
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        try {
            Product createdProduct = productService.create(product, merchantId);
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/merchant")
    public ResponseEntity<List<Product>> getProductsForMerchant(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        List<Product> products = productService.getAllByMerchant(tenantId);
        // REFACTORED: Let the client see an empty array, which is more standard than 204 No Content.
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(productService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @RequestBody ProductDTO product, @AuthenticationPrincipal Jwt jwt) {
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        try {
            Product updated = productService.update(id, product, merchantId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            // Can be more specific with custom exceptions for Not Found vs. Unauthorized
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        try {
            productService.delete(id, merchantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        return ResponseEntity.ok(productService.search(query));
    }
}