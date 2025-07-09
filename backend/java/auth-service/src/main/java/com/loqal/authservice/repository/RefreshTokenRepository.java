package com.loqal.authservice.repository;

import com.loqal.authservice.entity.RefreshToken;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByEmail(String email);

    Optional<RefreshToken> findByEmail(@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email);
}