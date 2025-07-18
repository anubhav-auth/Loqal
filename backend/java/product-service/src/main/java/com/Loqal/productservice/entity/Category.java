package com.Loqal.productservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Embeddable
public class Category {

    @Id

    private UUID id;
    private String name;
    private String description;
}
