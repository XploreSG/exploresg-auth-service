package com.exploresg.authservice.service;

import org.springframework.stereotype.Service;

import com.exploresg.authservice.model.User;
import com.exploresg.authservice.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User upsertUserFromJwt(Jwt jwt) {
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("name");
        String givenName = jwt.getClaim("given_name");
        String familyName = jwt.getClaim("family_name");
        String picture = jwt.getClaim("picture");
        String sub = jwt.getSubject();

        return userRepository.findByEmail(email)
                .map(existing -> {
                    // Optionally update other fields if changed
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
                                .build()));
    }
}
