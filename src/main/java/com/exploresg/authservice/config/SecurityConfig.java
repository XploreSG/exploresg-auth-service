package com.exploresg.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html",
                                                                "/ping",
                                                                "/hello",
                                                                "/health",
                                                                "/test/**",
                                                                "/api/v1/**",
                                                                "/api/v1/auth/log-token",
                                                                "/api/v1/auth/refresh")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                                .csrf(csrf -> csrf.disable());

                return http.build();
        }
}
