package com.exploresg.authservice.service;

import com.exploresg.authservice.model.UserEntity;
import com.exploresg.authservice.model.UserStatus;
import com.exploresg.authservice.model.UserRole;
import com.exploresg.authservice.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        // Validate required fields
        if (email == null || email.trim().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // 1. Persist or load UserEntity (capture the returned user)
        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        UserEntity.builder()
                                .email(email)
                                .authProvider("GOOGLE")
                                .userStatus(UserStatus.ACTIVE)
                                .userRole(UserRole.USER)
                                .name(name)
                                .pictureUrl(pictureUrl)
                                .createdAt(LocalDateTime.now())
                                .build()));

        // 2. Build GrantedAuthority from UserRole
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));

        // 3. Return DefaultOAuth2User with both attributes and authorities
        return new DefaultOAuth2User(
                authorities,
                oAuth2User.getAttributes(),
                "email");
    }
}
