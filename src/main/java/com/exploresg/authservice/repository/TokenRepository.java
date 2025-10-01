package com.exploresg.authservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exploresg.authservice.model.AuthToken;
import com.exploresg.authservice.model.User;

public interface TokenRepository extends JpaRepository<AuthToken, Long> {

    List<AuthToken> findAllByUserAndIsRevokedFalse(User user);

}
