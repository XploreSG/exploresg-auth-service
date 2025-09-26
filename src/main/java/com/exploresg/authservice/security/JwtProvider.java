package com.exploresg.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

@Component
public class JwtProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(userId) // Updated method
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now)) // Updated method
                .expiration(Date.from(expiration)) // Updated method
                .signWith(key, Jwts.SIG.HS256) // Updated signature method
                .compact();
    }

    /**
     * Validate JWT token - WITH NULL CHECKS
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token validation failed: token is null or empty");
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from token - WITH NULL CHECKS
     */
    public String getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot extract user ID: token is null or empty");
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract email from token - WITH NULL CHECKS
     */
    public String getEmailFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot extract email: token is null or empty");
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("email", String.class);
        } catch (JwtException e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract roles from token - WITH NULL CHECKS AND TYPE SAFETY
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot extract roles: token is null or empty");
            return Set.of();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof Set) {
                return (Set<String>) rolesObj;
            } else if (rolesObj instanceof java.util.List) {
                // JWT serialization converts Set to List, so handle List case
                return Set.copyOf((java.util.List<String>) rolesObj);
            } else {
                log.warn("Roles claim is not a Set or List: {}", rolesObj != null ? rolesObj.getClass() : "null");
                return Set.of();
            }
        } catch (JwtException e) {
            log.error("Failed to extract roles from token: {}", e.getMessage());
            return Set.of();
        } catch (ClassCastException e) {
            log.error("Failed to cast roles to Set<String>: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Check if token is expired - WITH NULL CHECKS
     */
    public boolean isTokenExpired(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot check expiration: token is null or empty");
            return true; // Treat null/empty as expired
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true; // Treat invalid tokens as expired
        }
    }
}