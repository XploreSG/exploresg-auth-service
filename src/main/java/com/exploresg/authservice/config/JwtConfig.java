package com.exploresg.authservice.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(@Value("${auth.jwt.secret}") String secret) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String googleIssuer) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder localDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .build();
        JwtDecoder googleDecoder = buildGoogleDecoder(googleIssuer);

        return token -> {
            try {
                return localDecoder.decode(token);
            } catch (JwtException ex) {
                return googleDecoder.decode(token);
            }
        };
    }

    private JwtDecoder buildGoogleDecoder(String issuer) {
        try {
            return JwtDecoders.fromIssuerLocation(issuer);
        } catch (Exception ex) {
            return token -> {
                throw new JwtException("Unable to decode Google token", ex);
            };
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");
        converter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(converter::convert);
        return authenticationConverter;
    }
}
