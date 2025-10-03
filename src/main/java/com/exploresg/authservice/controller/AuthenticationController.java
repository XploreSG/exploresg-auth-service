package com.exploresg.authservice.controller;

import com.exploresg.authservice.dto.AuthResponse;
import com.exploresg.authservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @RequestHeader("Authorization") String authorizationHeader) {
        String googleToken = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7)
                : authorizationHeader;
        AuthResponse authResponse = authenticationService.signInWithGoogle(googleToken);
        return ResponseEntity.ok(authResponse);
    }
}