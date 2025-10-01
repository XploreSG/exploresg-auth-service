package com.exploresg.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exploresg.authservice.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
