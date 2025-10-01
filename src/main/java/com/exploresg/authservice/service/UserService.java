package com.exploresg.authservice.service;

import org.springframework.stereotype.Service;

import com.exploresg.authservice.dto.SignupProfileRequest;
import com.exploresg.authservice.model.IdentityProvider;
import com.exploresg.authservice.model.Role;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.model.UserProfile;
import com.exploresg.authservice.repository.UserProfileRepository;
import com.exploresg.authservice.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User upsertUserFromJwt(Jwt jwt) {
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("name");
        String givenName = jwt.getClaim("given_name");
        String familyName = jwt.getClaim("family_name");
        String picture = jwt.getClaim("picture");
        String sub = jwt.getSubject();

        LocalDateTime now = LocalDateTime.now();

        return userRepository.findByEmail(email)
                .map(existing -> {
                    // Optionally update other fields if they've changed
                    boolean updated = false;
                    if (!name.equals(existing.getName())) {
                        existing.setName(name);
                        updated = true;
                    }
                    if (!givenName.equals(existing.getGivenName())) {
                        existing.setGivenName(givenName);
                        updated = true;
                    }
                    if (!familyName.equals(existing.getFamilyName())) {
                        existing.setFamilyName(familyName);
                        updated = true;
                    }
                    if (picture != null && !picture.equals(existing.getPicture())) {
                        existing.setPicture(picture);
                        updated = true;
                    }
                    if (!sub.equals(existing.getGoogleSub())) {
                        existing.setGoogleSub(sub);
                        updated = true;
                    }
                    if (updated) {
                        existing.setUpdatedAt(now); // Optional: Audit
                        userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .givenName(givenName)
                                .familyName(familyName)
                                .picture(picture)
                                .googleSub(sub)
                                .isActive(true)
                                .role(Role.USER)
                                .identityProvider(IdentityProvider.GOOGLE)
                                .createdAt(now)
                                .updatedAt(now)
                                .build()));
    }

    @Transactional
    public UserProfile createOrUpdateProfile(Long userId, SignupProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // --- Optional name overrides (signup form can override Google claims) ---
        boolean updatedUser = false;
        if (req.getGivenName() != null && !req.getGivenName().isBlank()
                && !req.getGivenName().equals(user.getGivenName())) {
            user.setGivenName(req.getGivenName());
            updatedUser = true;
        }
        if (req.getFamilyName() != null && !req.getFamilyName().isBlank()
                && !req.getFamilyName().equals(user.getFamilyName())) {
            user.setFamilyName(req.getFamilyName());
            updatedUser = true;
        }
        if (updatedUser) {
            user.setName(user.getGivenName() + " " + user.getFamilyName());
            userRepository.save(user);
        }

        // --- Profile create/update ---
        return userProfileRepository.findById(userId)
                .map(existing -> {
                    existing.setPhone(req.getPhone());
                    existing.setDateOfBirth(req.getDateOfBirth());
                    existing.setDrivingLicenseNumber(req.getDrivingLicenseNumber());
                    existing.setPassportNumber(req.getPassportNumber());
                    existing.setPreferredLanguage(req.getPreferredLanguage());
                    existing.setCountryOfResidence(req.getCountryOfResidence());
                    return userProfileRepository.save(existing);
                })
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .user(user)
                                .phone(req.getPhone())
                                .dateOfBirth(req.getDateOfBirth())
                                .drivingLicenseNumber(req.getDrivingLicenseNumber())
                                .passportNumber(req.getPassportNumber())
                                .preferredLanguage(req.getPreferredLanguage())
                                .countryOfResidence(req.getCountryOfResidence())
                                .build()));
    }

}
