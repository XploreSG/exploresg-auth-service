package com.exploresg.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.exploresg.authservice.dto.TokenPairResponse;
import com.exploresg.authservice.model.IdentityProvider;
import com.exploresg.authservice.model.Role;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.repository.TokenRepository;
import com.exploresg.authservice.repository.UserRepository;

@SpringBootTest
@Transactional
class TokenServiceTests {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Test
    void generateTokenPairCreatesRefreshRecord() {
        User user = userRepository.save(User.builder()
                .email("pair@test.example")
                .name("Pair Test")
                .givenName("Pair")
                .familyName("Test")
                .picture(null)
                .googleSub("pair-sub")
                .isActive(true)
                .role(Role.USER)
                .identityProvider(IdentityProvider.GOOGLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        TokenPairResponse pair = tokenService.generateTokenPair(user);

        assertThat(pair.getAccessToken()).isNotBlank();
        assertThat(pair.getRefreshToken()).isNotBlank();
        assertThat(tokenRepository.findAllByUserAndIsRevokedFalse(user)).hasSize(1);
    }

    @Test
    void refreshTokenRevokesOldToken() {
        User user = userRepository.save(User.builder()
                .email("refresh@test.example")
                .name("Refresh Test")
                .givenName("Refresh")
                .familyName("Test")
                .picture(null)
                .googleSub("refresh-sub")
                .isActive(true)
                .role(Role.USER)
                .identityProvider(IdentityProvider.GOOGLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        TokenPairResponse initial = tokenService.generateTokenPair(user);
        TokenPairResponse refreshed = tokenService.refreshToken(initial.getRefreshToken());

        assertThat(refreshed.getAccessToken()).isNotEqualTo(initial.getAccessToken());
        assertThat(tokenRepository.findAllByUserAndIsRevokedFalse(user)).hasSize(1);

        assertThatThrownBy(() -> tokenService.refreshToken(initial.getRefreshToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
