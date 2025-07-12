package com.Loqal.userservice.repository;

import com.Loqal.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<Object> findByEmail(String email);
}
