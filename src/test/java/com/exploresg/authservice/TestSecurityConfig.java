package com.exploresg.authservice;

import java.time.Instant;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Configuration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                Map<String, Object> headers = Map.of("alg", "none");
                Map<String, Object> claims = Map.of(
                        "sub", "test-sub",
                        "email", "test@example.com",
                        "name", "Test User");
                Instant now = Instant.now();
                return new Jwt(token, now, now.plusSeconds(3600), headers, claims);
            }
        };
    }
}
