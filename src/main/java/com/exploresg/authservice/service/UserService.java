package com.exploresg.authservice.service;

import org.springframework.stereotype.Service;

import com.exploresg.authservice.model.IdentityProvider;
import com.exploresg.authservice.model.Role;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

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
}
