package com.Loqal.ProductService.controller;

import com.Loqal.ProductService.entity.Product;
import com.Loqal.ProductService.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Product>> getByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(productService.getProductsByMerchant(merchantId));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @PutMapping("/{id}/inventory")
    public ResponseEntity<Product> updateInventory(@PathVariable String id, @RequestParam int quantity) {
        return ResponseEntity.ok(productService.updateInventory(id, quantity));
    }
}

