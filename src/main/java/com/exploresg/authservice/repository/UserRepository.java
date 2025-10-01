package com.exploresg.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exploresg.authservice.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email); // auto-implemented by Spring

    Optional<User> findByEmail(String email);
}
