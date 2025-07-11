package com.Loqal.ProductService.services;

import com.Loqal.ProductService.entity.Product;
import com.Loqal.ProductService.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByMerchant(String merchantId) {
        return productRepository.findByMerchantId(merchantId);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public Product updateInventory(String productId, int newInventory) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setInventory(newInventory);
            return productRepository.save(product);
        }
        return null;
    }
}
