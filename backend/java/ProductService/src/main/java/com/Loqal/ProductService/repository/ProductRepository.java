package com.Loqal.ProductService.repository;

import com.Loqal.ProductService.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

}
