package com.Loqal.productservice.services;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.entity.ProductDTO;
import com.Loqal.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {
    @Autowired
    private ProductRepository repo;

    public ResponseEntity<Product> create(ProductDTO p, UUID merchantID, Date created, Date updated) {

        Product pd = new Product();
        pd.setName(p.name());
        pd.setDescription(p.description());
        pd.setCategory(p.category());
        pd.setPrice(p.price());
        pd.setMerchantId(merchantID);
        pd.setQuantity(p.quantity());
        pd.setImage_urls(p.image_urls());
        pd.setCreated_at(created);
        pd.setUpdated_at(updated);

        return ResponseEntity.ok(repo.save(pd));

        ///
    }



    public ResponseEntity<List<Product>> getAll(UUID tenenant_id) {
        return ResponseEntity.ok(repo.findAll());
    }

    public Optional<Product> getById(UUID id) {

        return repo.findById(id);
    }

    public Product update(UUID id, ProductDTO updated) {
        return repo.findById(id).map(existing -> {
            existing.setName(updated.name());
            existing.setDescription(updated.description());
            existing.setCategory(updated.category());
            existing.setPrice(updated.price());
            existing.setQuantity(updated.quantity());
            existing.setImage_urls(updated.image_urls());
            existing.setUpdated_at(new Date());
            return repo.save(existing);
        }).orElse(null);
    }
}

