package com.loqal.authservice.repository;

import com.loqal.authservice.entity.RefreshToken;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, UUID> {
    Mono<RefreshToken> findByToken(String token);

    Mono<Void> deleteByEmail(String email);

    Mono<RefreshToken> findByEmail(@NotBlank @Email String email);
}