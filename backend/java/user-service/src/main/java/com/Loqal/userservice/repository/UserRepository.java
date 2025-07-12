package com.Loqal.userservice.repository;

import com.Loqal.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<Object> findByEmail(String email);
}
