package com.loqal.authservice.repository;

import com.loqal.authservice.entity.RefreshToken;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, UUID> {
    Mono<RefreshToken> findByToken(String token);

    Mono<RefreshToken> findByEmail(String email);

    @Query("INSERT INTO refresh_tokens (id, token, email, expiration) VALUES ($1, $2, $3, $4) RETURNING *")
    Mono<RefreshToken> insertNewToken(UUID id, String token, String email, Instant expiration);

    @Query("UPDATE refresh_tokens SET token = $2, expiration = $3 WHERE email = $1 RETURNING *")
    Mono<RefreshToken> updateTokenByEmail(String email, String token, Instant expiration);
}