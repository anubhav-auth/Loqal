package com.Loqal.productservice.repository;

import com.Loqal.productservice.entity.ProcessedEvent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends R2dbcRepository<ProcessedEvent, UUID> {
}