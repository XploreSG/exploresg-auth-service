package com.exploresg.authservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exploresg.authservice.dto.AuthSessionResponse;
import com.exploresg.authservice.dto.RefreshTokenRequest;
import com.exploresg.authservice.dto.SignupProfileRequest;
import com.exploresg.authservice.dto.SignupResponse;
import com.exploresg.authservice.dto.TokenPairResponse;
import com.exploresg.authservice.model.Role;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.model.UserProfile;
import com.exploresg.authservice.service.TokenService;
import com.exploresg.authservice.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

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
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        System.out.println("Received token: " + token);
        return ResponseEntity.ok("Token logged successfully");
    }

    @PostMapping("/auth/session")
    public ResponseEntity<AuthSessionResponse> createSession(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.upsertUserFromJwt(jwt, null);
        TokenPairResponse tokens = tokenService.generateTokenPair(user);
        AuthSessionResponse response = mapToSessionResponse(user, tokens);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenPairResponse> refreshTokens(@Valid @RequestBody RefreshTokenRequest request) {
        TokenPairResponse tokens = tokenService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.upsertUserFromJwt(jwt, null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signupProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SignupProfileRequest request) {

        Role requestedRole = request.getRequestedRole();
        User user = userService.upsertUserFromJwt(jwt, requestedRole);

        UserProfile profile = userService.createOrUpdateProfile(user.getId(), request);

        TokenPairResponse tokens = tokenService.generateTokenPair(user);

        SignupResponse response = new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getGivenName(),
                user.getFamilyName(),
                user.getPicture(),
                profile.getPhone(),
                profile.getDateOfBirth(),
                profile.getDrivingLicenseNumber(),
                profile.getPassportNumber(),
                profile.getPreferredLanguage(),
                profile.getCountryOfResidence(),
                tokens);

        return ResponseEntity.ok(response);
    }

    private AuthSessionResponse mapToSessionResponse(User user, TokenPairResponse tokens) {
        return new AuthSessionResponse(
                user.getId(),
                user.getEmail(),
                user.getGivenName(),
                user.getFamilyName(),
                user.getPicture(),
                user.getRole(),
                tokens);
    }
}
