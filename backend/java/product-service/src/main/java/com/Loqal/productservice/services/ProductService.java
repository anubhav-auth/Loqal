package com.Loqal.productservice.services;

import com.Loqal.productservice.entity.Product;
import com.Loqal.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {
    @Autowired private ProductRepository repo;

    public Product create(Product p) {
        p.setCreated_at(new Date());
        p.setUpdated_at(new Date());
        return repo.save(p);
    }

    public List<Product> getAll() {
        return repo.findAll();
    }

    public Optional<Product> getById(UUID id) {
        return repo.findById(id);
    }

    public Product update(UUID id, Product updated) {
        return repo.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            existing.setCategory(updated.getCategory());
            existing.setPrice(updated.getPrice());
            existing.set_available(updated.is_available());
            existing.setImage_urls(updated.getImage_urls());
            existing.setUpdated_at(new Date());
            return repo.save(existing);
        }).orElse(null);
    }
}

