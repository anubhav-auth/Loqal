package com.Loqal.ProductService.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
@Data
public class Product {
    @Id
    private String id;
    private String name;//product name
    private String description;//details about the product
    private String category;// ex-accessories
    private double price;
    private int inventory; //Number of units available in stock.
    private String merchantId;//ID of the merchant who owns this product. Useful for filtering per merchant.
}
