package com.Loqal.productservice.controller;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.repository.ProductRepository;
import com.Loqal.productservice.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService  productService;
    @Autowired
    private ProductRepository repository;

    @PostMapping
    public Product create(@RequestBody ProductDTO product, @AuthenticationPrincipal Jwt jwt, Date createdAt,Date updatedAt) {
        UUID tenenat_id = UUID.fromString(jwt.getClaimAsString("tenent_id"));

        return productService.create(product, tenenat_id,createdAt,updatedAt).getBody();
    }
    @PostMapping("/check-and-update-stock")
    public ResponseEntity<?> checkAndUpdateStock(@RequestBody List<ProductOrderRequest> orderRequests) {
        List<String> errors = new ArrayList<>();

        for (ProductOrderRequest request : orderRequests) {
            Optional<Product> optionalProduct = repository.findById(request.getProductId());

            if (optionalProduct.isEmpty()) {
                errors.add("Product not found: " + request.getProductId());
                return ResponseEntity.notFound().build();

            }

            Product product = optionalProduct.get();

            if (product.getQuantity() < request.getQuantity()) {
                errors.add("Insufficient stock for product: " + product.getName());
                return ResponseEntity.badRequest().body("not enough items present");

            }


            product.setQuantity(product.getQuantity() - request.getQuantity());
            repository.save(product);
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors);
        }

        return ResponseEntity.ok("Stock updated successfully.");
    }


    @GetMapping
    public ResponseEntity<?> all(@AuthenticationPrincipal Jwt jwt) {
        UUID tenenat_id = UUID.fromString(jwt.getClaimAsString("tenent_id"));

        if (tenenat_id == null) return ResponseEntity.internalServerError().body("tenenat_if is not avaialble");

        return ResponseEntity.ok(productService.getAll(tenenat_id)) ;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable UUID id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable UUID id, @RequestBody ProductDTO product) {
        Product updated = productService.update(id, product);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

}
