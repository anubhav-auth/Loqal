package com.loqal.authservice.repository;

import com.loqal.authservice.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    Optional<UserCredential> findByEmail(String email);
    void deleteByEmail(String email);
}
