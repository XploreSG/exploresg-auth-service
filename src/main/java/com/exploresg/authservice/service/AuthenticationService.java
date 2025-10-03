package com.exploresg.authservice.service;

import com.exploresg.authservice.dto.AuthResponse;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.model.UserProfile;
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

    @Transactional
    public AuthResponse signInWithGoogle(String googleToken) {
        try {
            Jwt jwt = jwtDecoder.decode(googleToken);
            User user = userService.upsertUserFromJwt(jwt, null);
            String customToken = customJwtService.generateToken(user);

            // Find the user's profile, if it exists
            UserProfile userProfile = userProfileRepository.findById(user.getId()).orElse(null);

            // Build the UserInfo DTO
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .givenName(user.getGivenName())
                    .familyName(user.getFamilyName())
                    .picture(user.getPicture())
                    .phone(userProfile != null ? userProfile.getPhone() : null)
                    .dateOfBirth(userProfile != null ? userProfile.getDateOfBirth().toString() : null)
                    .drivingLicenseNumber(userProfile != null ? userProfile.getDrivingLicenseNumber() : null)
                    .passportNumber(userProfile != null ? userProfile.getPassportNumber() : null)
                    .preferredLanguage(userProfile != null ? userProfile.getPreferredLanguage() : null)
                    .countryOfResidence(userProfile != null ? userProfile.getCountryOfResidence() : null)
                    .build();

            return AuthResponse.builder()
                    .token(customToken)
                    .requiresProfileSetup(userProfile == null)
                    .userInfo(userInfo) // <-- Include the user info in the response
                    .build();

        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid Google token: " + e.getMessage());
        }
    }
}