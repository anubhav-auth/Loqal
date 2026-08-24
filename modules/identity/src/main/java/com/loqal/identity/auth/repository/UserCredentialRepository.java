package com.loqal.identity.auth.repository;

import com.loqal.identity.auth.entity.UserCredential;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UserCredentialRepository extends ReactiveCrudRepository<UserCredential, UUID> {
    Mono<UserCredential> findByEmail(String email);

    @Query("INSERT INTO user_credentials (id, email, password_hash, created_at) VALUES ($1, $2, $3, $4) RETURNING *")
    Mono<UserCredential> insertNewUser(UUID id, String email, String passwordHash, LocalDateTime createdAt);
}
