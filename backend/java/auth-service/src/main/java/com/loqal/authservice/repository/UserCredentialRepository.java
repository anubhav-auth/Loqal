package com.loqal.authservice.repository;

import com.loqal.authservice.entity.UserCredential;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserCredentialRepository extends ReactiveCrudRepository<UserCredential, UUID> {
    Mono<UserCredential> findByEmail(String email);

    Mono<Void> deleteByEmail(String email);
}
