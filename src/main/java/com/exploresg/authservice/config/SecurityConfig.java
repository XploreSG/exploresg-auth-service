package com.exploresg.authservice.config;

import com.exploresg.authservice.security.CustomOAuth2SuccessHandler;
import com.exploresg.authservice.security.JwtAuthenticationFilter;
import com.exploresg.authservice.security.JwtProvider;
import com.exploresg.authservice.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
        private final JwtProvider jwtProvider;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                        CustomOAuth2SuccessHandler customOAuth2SuccessHandler,
                        JwtProvider jwtProvider) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
                this.jwtProvider = jwtProvider;
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter(jwtProvider);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints (no authentication required)
                                                .requestMatchers("/login", "/oauth2/**", "/login/oauth2/code/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/health",
                                                                "/api/v1/auth/validate")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // Swagger UI and API docs
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**", "/webjars/**")
                                                .permitAll()
                                                // H2 Database console
                                                .requestMatchers("/h2-console/**").permitAll()
                                                // Protected API endpoints (require JWT authentication)
                                                .requestMatchers("/api/v1/auth/me").authenticated()
                                                .requestMatchers("/api/v1/auth/logout").authenticated()
                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(customOAuth2SuccessHandler))
                                // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
                                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allow specific origins (customize these for your frontend URLs)
                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000", // React default
                                "http://localhost:4200", // Angular default
                                "http://localhost:8080", // Vue/general frontend
                                "http://localhost:5173", // Vite default
                                "https://your-frontend-domain.com" // Production frontend
                ));

                // Allow common HTTP methods
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

                // Allow common headers
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization", "Content-Type", "X-Requested-With",
                                "Accept", "Origin", "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                // Allow credentials (important for OAuth2)
                configuration.setAllowCredentials(true);

                // Cache preflight requests for 1 hour
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
