package com.loqal.identity.users.repository;

import com.loqal.identity.users.entity.User;
import com.loqal.identity.users.entity.dto.UserRoles;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    Mono<User> findByEmail(String email);

    @Query("""
        INSERT INTO users (id, email, full_name, phone_number, profile_picture_url, roles, street, city, state, postal_code, country, tenant_id, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
        RETURNING *
    """)
    Mono<User> insertNewUser(UUID id, String email, String fullName, String phoneNumber, String profilePictureUrl, String[] roles, String street, String city, String state, String postalCode, String country, UUID tenantId, LocalDateTime createdAt, LocalDateTime updatedAt);
}