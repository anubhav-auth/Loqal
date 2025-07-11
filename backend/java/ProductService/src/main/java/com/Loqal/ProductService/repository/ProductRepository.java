package com.Loqal.ProductService.repository;

import com.Loqal.ProductService.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByMerchantId(String merchantId);
    List<Product> findByCategory(String category);
}
