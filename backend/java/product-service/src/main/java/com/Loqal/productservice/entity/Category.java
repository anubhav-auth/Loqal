package com.Loqal.productservice.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Category {


    private String category_name;
    private String category_description;
}
