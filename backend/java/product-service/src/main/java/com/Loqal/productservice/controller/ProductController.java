package com.Loqal.productservice.controller;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.repository.ProductRepository;
import com.Loqal.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping()
public class ProductController {

    private final ProductService  productService;

    @PostMapping("/internal/create")
    public ResponseEntity<?> create(@RequestBody ProductDTO product, @AuthenticationPrincipal Jwt jwt) {
        if (product == null || product.name() == null || product.price() <= 0 || product.quantity() < 0) {
            return ResponseEntity.badRequest().body("Invalid product data provided.");
        }
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenent_id"));
        try{
            return ResponseEntity.ok(productService.create(product, merchantId)).getBody();
        }catch(Exception e){
            return ResponseEntity.badRequest().body("Error creating product: " + e.getMessage());
        }
    }


    @PostMapping("/internal/order")
    public ResponseEntity<?> checkAndUpdateStock(@RequestBody List<ProductOrderRequest> orderRequests) {
        if (orderRequests == null || orderRequests.isEmpty()) {
            return ResponseEntity.badRequest().body("No order requests provided.");
        }
        return ResponseEntity.ok(productService.checkOrderAndUpdateStock(orderRequests));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts(UUID tenant_id) {
        return ResponseEntity.ok(productService.getAll(tenant_id)) ;
    }

    @GetMapping("/internal/products")
    public ResponseEntity<?> getAllProducts(@AuthenticationPrincipal Jwt jwt) {
        UUID tenant_id = UUID.fromString(jwt.getClaimAsString("tenant_id"));

        return ResponseEntity.ok(productService.getAll(tenant_id)) ;
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getById(@PathVariable UUID id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("internal/product/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ProductDTO product, @AuthenticationPrincipal Jwt jwt) {
        if (product == null || product.name() == null || product.price() <= 0 || product.quantity() < 0) {
            return ResponseEntity.badRequest().body("Invalid product data provided.");
        }
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        Product updated = productService.update(id, product, merchantId);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/internal/product/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID merchantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        productService.delete(id, merchantId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        return ResponseEntity.ok(productService.search(query));
    }

}