package com.Loqal.productservice.services;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.entity.ProductOrderRequest;
import com.Loqal.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;

    public ResponseEntity<?> create(ProductDTO p, UUID merchantID) {
        try {
            Product pd = new Product();
            pd.setName(p.name());
            pd.setDescription(p.description());
            pd.setCategory(p.category());
            pd.setPrice(p.price());
            pd.setMerchantId(merchantID);
            pd.setQuantity(p.quantity());
            pd.setImage_urls(p.image_urls());
            pd.setCreated_at(LocalDateTime.now());

            return ResponseEntity.ok(repo.save(pd));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public ResponseEntity<?> checkOrderAndUpdateStock(List<ProductOrderRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().body("No order requests provided.");
        }

        for (ProductOrderRequest request : requests) {
            Optional<Product> productOpt = repo.findById(request.getProductId());
            if (productOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Product with ID " + request.getProductId() + " not found.");
            }

            Product product = productOpt.get();
            if (product.getQuantity() < request.getQuantity()) {
                return ResponseEntity.badRequest().body("Insufficient stock for product ID " + request.getProductId());
            }

            product.setQuantity(product.getQuantity() - request.getQuantity());
            product.setUpdated_at(LocalDateTime.now());
            repo.save(product);
        }

        return ResponseEntity.ok("Stock updated successfully.");

    }


    public ResponseEntity<List<Product>> getAll(UUID tenant_id) {
        return ResponseEntity.ok(repo.findAllByMerchantId(tenant_id));
    }

    public Optional<Product> getById(UUID id) {
        return repo.findById(id);
    }

    public Product update(UUID id, ProductDTO updated, UUID tenant_id) {
        return repo.findById(id).map(existing -> {
            if(!existing.getMerchantId().equals(tenant_id)) {
                throw new IllegalArgumentException("Unauthorized to update this product");
            }
            existing.setName(updated.name());
            existing.setDescription(updated.description());
            existing.setCategory(updated.category());
            existing.setPrice(updated.price());
            existing.setQuantity(updated.quantity());
            existing.setImage_urls(updated.image_urls());
            existing.setUpdated_at(LocalDateTime.now());
            return repo.save(existing);
        }).orElse(null);
    }
}

