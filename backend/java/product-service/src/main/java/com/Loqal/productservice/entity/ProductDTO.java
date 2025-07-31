package com.Loqal.productservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;


public record ProductDTO(

    String name,

    String description,

    Category category,

    double price,

    int quantity,

    List<String> image_urls
){}

