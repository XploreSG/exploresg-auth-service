package com.exploresg.authservice.controller;

import com.exploresg.authservice.model.UserEntity;
import com.exploresg.authservice.repository.UserRepository;
import com.exploresg.authservice.security.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Tag(name = "Authentication", description = "Google OAuth2 + JWT Authentication endpoints")
public class GoogleAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthController.class);

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public GoogleAuthController(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    /**
     * Redirect to Google OAuth2 login
     * GET /api/v1/auth/login
     */
    @Operation(summary = "Initiate Google OAuth2 login", description = "Redirects to Google OAuth2 authorization page. After successful login, user will be redirected back with a JWT token.")
    @ApiResponse(responseCode = "302", description = "Redirect to Google OAuth2 login page")
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        // Redirect to Spring Security's OAuth2 login endpoint for Google
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Get current authenticated user information
     * GET /api/v1/auth/me
     * Requires: Authorization: Bearer <jwt_token>
     */
    @Operation(summary = "Get current user information", description = "Returns the authenticated user's profile information from JWT token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "User information retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            // Check if user is authenticated (JWT filter should have set this)
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            // Get user email from authentication (set by JWT filter)
            String email = authentication.getName();
            log.debug("Getting user info for authenticated email: {}", email);

            // Get user details from database
            UserEntity user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Return user information (without sensitive data)
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getName());
            userInfo.put("pictureUrl", user.getPictureUrl());
            userInfo.put("role", user.getUserRole().name());
            userInfo.put("status", user.getUserStatus().name());
            userInfo.put("createdAt", user.getCreatedAt());
            userInfo.put("authenticated", true);

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("Failed to get user information", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get user information",
                    "details", e.getMessage()));
        }
    }

    /**
     * Validate a JWT token
     * POST /api/v1/auth/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            log.info("Token validation request received. Token present: {}", token != null);

            if (token == null || token.trim().isEmpty()) {
                log.warn("Token validation failed: token is null or empty");
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "error", "Token is required"));
            }

            boolean isValid = jwtProvider.validateToken(token);
            log.info("Token validation result: {}", isValid);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            if (isValid) {
                try {
                    // Extract basic info from token
                    response.put("userId", jwtProvider.getUserIdFromToken(token));
                    response.put("email", jwtProvider.getEmailFromToken(token));
                    response.put("roles", jwtProvider.getRolesFromToken(token));
                } catch (Exception e) {
                    log.error("Error extracting info from valid token", e);
                    response.put("valid", true); // Token is valid but info extraction failed
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token validation failed with exception", e); // This will show the actual error
            return ResponseEntity.status(500).body(Map.of(
                    "valid", false,
                    "error", "Failed to validate token",
                    "details", e.getMessage() // Include actual error message
            ));
        }
    }

    /**
     * Logout endpoint (for frontend to call)
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Since we're using stateless JWT, logout is handled on the frontend
        // by removing the token from storage. This endpoint just confirms logout.
        return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "instruction", "Remove JWT token from client storage"));
    }

    /**
     * Get authentication status
     * GET /api/v1/auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "email", oauth2User.getAttribute("email"),
                    "name", oauth2User.getAttribute("name")));
        } else {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/auth/health
     */
    @Operation(summary = "Service health check", description = "Returns the current status of the authentication service")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "exploresg-auth-service",
                "timestamp", System.currentTimeMillis()));
    }
}
