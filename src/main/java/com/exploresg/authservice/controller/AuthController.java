package com.exploresg.authservice.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exploresg.authservice.model.User;
import com.exploresg.authservice.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/check")
    public ResponseEntity<?> checkUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = jwt.getClaim("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No email in token"));
        }

        boolean exists = userService.existsByEmail(email);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "email", email));
    }

    @PostMapping("/auth/log-token")
    public ResponseEntity<?> logToken(@RequestHeader("Authorization") String authHeader) {
        // Remove "Bearer " prefix if present
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        System.out.println("Received token: " + token);
        // You can return a simple response for now
        return ResponseEntity.ok("Token logged successfully");
    }

    // @GetMapping("/me")
    // public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
    // String email = jwt.getClaim("email");
    // String sub = jwt.getSubject();
    // System.err.println("SSO email: " + email + " google sub: " + sub);

    // return ResponseEntity.ok(
    // java.util.Map.of("email", email, "sub", sub, "jwt", jwt.getTokenValue()));
    // }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.upsertUserFromJwt(jwt);
        return ResponseEntity.ok(user);
    }
}
