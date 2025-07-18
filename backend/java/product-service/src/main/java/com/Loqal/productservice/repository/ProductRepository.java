package com.Loqal.productservice.repository;

import com.Loqal.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

}
