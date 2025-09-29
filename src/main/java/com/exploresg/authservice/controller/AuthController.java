package com.exploresg.authservice.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class AuthController {
    @PostMapping("/api/auth/log-token")
    public ResponseEntity<?> logToken(@RequestHeader("Authorization") String authHeader) {
        // Remove "Bearer " prefix if present
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        System.out.println("Received token: " + token);
        // You can return a simple response for now
        return ResponseEntity.ok("Token logged successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaim("email");
        String sub = jwt.getSubject();
        System.err.println("SSO email: " + email + " google sub: " + sub);

        return ResponseEntity.ok(
                java.util.Map.of("email", email, "sub", sub, "jwt", jwt.getTokenValue()));
    }
}
