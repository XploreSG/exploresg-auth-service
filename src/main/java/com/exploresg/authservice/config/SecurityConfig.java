package com.exploresg.authservice.config;

import com.exploresg.authservice.service.JwtService; // Import JwtService
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService; // Import UserDetailsService
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final AuthenticationProvider authenticationProvider;
        private final JwtService jwtService;
        private final UserDetailsService userDetailsService;

        @Value("${cors.allowed-origins:http://localhost:3000}")
        private String allowedOrigins;

        @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
        private String allowedMethods;

        @Value("${cors.allowed-headers:*}")
        private String allowedHeaders;

        @Value("${cors.allow-credentials:true}")
        private boolean allowCredentials;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/error",
                                                                "/api/v1/check/**",
                                                                "/api/v1/auth/**",
                                                                "/api/v1/test/**", // ! TODO: Test endpoints for
                                                                                   // development
                                                                "/actuator/health",
                                                                "/actuator/health/liveness",
                                                                "/actuator/health/readiness",
                                                                "/actuator/info",
                                                                "/actuator/prometheus",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .anyRequest()
                                                .authenticated())
                                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                // Manually instantiate the filter here instead of injecting it
                                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
                configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
                configuration.setAllowCredentials(allowCredentials);
                configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
                configuration.setMaxAge(3600L); // Cache preflight for 1 hour
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}