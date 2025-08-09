package com.loqal.merchantservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.UUID;


@Entity
@Table(name = "merchants_extended")
@Data
public class MerchantExtended {
    @Id
    UUID id;
    UUID userId;
    String name;
    String description;
    String address;
    String logoUrl;
}