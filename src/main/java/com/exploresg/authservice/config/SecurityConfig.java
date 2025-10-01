package com.exploresg.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // allow swagger, v3 docs, actuator health, and some public endpoints without
                                // auth
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html",
                                                                "/ping",
                                                                "/hello",
                                                                "/health",
                                                                "/test/**",
                                                                "/api/**",
                                                                "/api/auth/log-token")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                // configure as an OAuth2 resource server using JWT (keeps existing behavior)
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt())
                                // disable CSRF for simplicity for now (commonly disabled for APIs)
                                .csrf(csrf -> csrf.disable());

                return http.build();
        }
}
