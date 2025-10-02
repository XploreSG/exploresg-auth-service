package com.exploresg.authservice.service;

import com.exploresg.authservice.dto.AuthResponse;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure this import is present

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    private final JwtService customJwtService;
    private final UserProfileRepository userProfileRepository;

    // --- THIS IS THE FIX ---
    // Change @Transactional(readOnly = true) to just @Transactional
    @Transactional
    public AuthResponse signInWithGoogle(String googleToken) {
        try {
            // 1. Decode and validate the Google token
            Jwt jwt = jwtDecoder.decode(googleToken);

            // 2. Create or update the user in your database (this requires a write
            // transaction)
            User user = userService.upsertUserFromJwt(jwt, null);

            // 3. Generate your own custom JWT for this user
            String customToken = customJwtService.generateToken(user);

            // 4. Check if the user's profile exists
            boolean profileExists = userProfileRepository.existsById(user.getId());

            // 5. Return the custom token and the profile status
            return AuthResponse.builder()
                    .token(customToken)
                    .requiresProfileSetup(!profileExists)
                    .build();

        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid Google token: " + e.getMessage());
        }
    }
}