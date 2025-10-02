package com.exploresg.authservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import com.exploresg.authservice.dto.TokenPairResponse;
import com.exploresg.authservice.model.AuthToken;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.repository.TokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtEncoder jwtEncoder;
    private final Clock clock = Clock.systemUTC();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    @Value("${auth.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    @Transactional
    public TokenPairResponse generateTokenPair(User user) {
        revokeActiveTokens(user);

        Instant issuedAt = Instant.now(clock);
        Instant accessExpiresAt = issuedAt.plus(Duration.ofMinutes(accessTokenMinutes));
        Instant refreshExpiresAt = issuedAt.plus(Duration.ofDays(refreshTokenDays));

        String accessToken = buildAccessToken(user, issuedAt, accessExpiresAt);
        String rawRefreshToken = buildRefreshToken();
        String hashedRefreshToken = hashToken(rawRefreshToken);

        AuthToken refreshTokenEntity = AuthToken.builder()
                .user(user)
                .tokenValue(hashedRefreshToken)
                .expiresAt(toLocalDateTime(refreshExpiresAt))
                .isRevoked(false)
                .build();
        tokenRepository.save(refreshTokenEntity);

        return new TokenPairResponse(accessToken, accessExpiresAt, rawRefreshToken, refreshExpiresAt);
    }

    @Transactional
    public TokenPairResponse refreshToken(String refreshToken) {
        String hashed = hashToken(refreshToken);
        AuthToken stored = tokenRepository.findByTokenValueAndIsRevokedFalse(hashed)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
        if (stored.getExpiresAt().isBefore(now)) {
            stored.setRevoked(true);
            tokenRepository.save(stored);
            throw new IllegalArgumentException("Refresh token expired");
        }

        stored.setRevoked(true);
        tokenRepository.save(stored);

        return generateTokenPair(stored.getUser());
    }

    private void revokeActiveTokens(User user) {
        List<AuthToken> activeTokens = tokenRepository.findAllByUserAndIsRevokedFalse(user);
        if (activeTokens.isEmpty()) {
            return;
        }
        activeTokens.forEach(token -> token.setRevoked(true));
        tokenRepository.saveAll(activeTokens);
    }

    private String buildAccessToken(User user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("exploresg-auth-service")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getGoogleSub() != null ? user.getGoogleSub() : String.valueOf(user.getId()))
                .claim("user_id", user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("given_name", user.getGivenName())
                .claim("family_name", user.getFamilyName())
                .claim("picture", user.getPicture())
                .claim("roles", List.of(user.getRole().name()))
                .claim("identity_provider", user.getIdentityProvider().name())
                .build();

        JwtEncoderParameters parameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims);
        return jwtEncoder.encode(parameters).getTokenValue();
    }

    private String buildRefreshToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
