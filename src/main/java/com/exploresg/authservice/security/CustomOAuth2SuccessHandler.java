package com.exploresg.authservice.security;

import com.exploresg.authservice.model.UserEntity;
import com.exploresg.authservice.repository.UserRepository;
import com.exploresg.authservice.security.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public CustomOAuth2SuccessHandler(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");

            log.info("OAuth2 authentication successful for user: {}", email);

            // Find user in database (should exist from CustomOAuth2UserService)
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after OAuth2 authentication"));

            // Generate JWT token
            String token = jwtProvider.generateToken(
                    user.getId().toString(),
                    user.getEmail(),
                    Set.of("ROLE_" + user.getUserRole().name()));

            // Build redirect URL with token and user info (enhanced from Comet's
            // suggestion)
            String redirectUrl = String.format(
                    "%s/auth/success?token=%s&userId=%s&email=%s&name=%s",
                    frontendUrl,
                    URLEncoder.encode(token, StandardCharsets.UTF_8),
                    user.getId(),
                    URLEncoder.encode(email, StandardCharsets.UTF_8),
                    URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8));

            log.info("Redirecting to frontend: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication success handling failed", e);
            String errorUrl = frontendUrl + "/login?error=" +
                    URLEncoder.encode("Authentication failed", StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }
}