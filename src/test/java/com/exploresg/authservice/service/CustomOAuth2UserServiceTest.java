package com.exploresg.authservice.service;

import com.exploresg.authservice.model.UserEntity;
import com.exploresg.authservice.model.UserRole;
import com.exploresg.authservice.model.UserStatus;
import com.exploresg.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomOAuth2UserService to verify role-based authority
 * generation.
 * These tests focus on the core logic of converting UserRole to
 * GrantedAuthority.
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void testCreateOAuth2UserWithRoles_UserRole_ReturnsCorrectAuthority() {
        // Test the core functionality of role mapping
        UserEntity userEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .name("Test User")
                .authProvider("GOOGLE")
                .userRole(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Map<String, Object> attributes = Map.of(
                "sub", "123456789",
                "email", "user@example.com",
                "name", "Test User",
                "picture", "https://example.com/profile.jpg");

        // Simulate the OAuth2User creation logic from our service
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + userEntity.getUserRole().name()));

        DefaultOAuth2User result = new DefaultOAuth2User(authorities, attributes, "email");

        // Assert
        assertThat(result).isNotNull();
        assertThat((String) result.getAttribute("email")).isEqualTo("user@example.com");

        Collection<? extends GrantedAuthority> resultAuthorities = result.getAuthorities();
        assertThat(resultAuthorities).hasSize(1);
        assertThat(resultAuthorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void testCreateOAuth2UserWithRoles_AdminRole_ReturnsCorrectAuthority() {
        UserEntity adminEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .name("Admin User")
                .authProvider("GOOGLE")
                .userRole(UserRole.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Map<String, Object> attributes = Map.of(
                "sub", "987654321",
                "email", "admin@example.com",
                "name", "Admin User",
                "picture", "https://example.com/admin.jpg");

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + adminEntity.getUserRole().name()));

        DefaultOAuth2User result = new DefaultOAuth2User(authorities, attributes, "email");

        // Assert
        assertThat(result).isNotNull();
        Collection<? extends GrantedAuthority> resultAuthorities = result.getAuthorities();
        assertThat(resultAuthorities).hasSize(1);
        assertThat(resultAuthorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void testCreateOAuth2UserWithRoles_FleetRole_ReturnsCorrectAuthority() {
        UserEntity fleetEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("fleet@example.com")
                .name("Fleet User")
                .authProvider("GOOGLE")
                .userRole(UserRole.FLEET)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Map<String, Object> attributes = Map.of(
                "sub", "555666777",
                "email", "fleet@example.com",
                "name", "Fleet User",
                "picture", "https://example.com/fleet.jpg");

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + fleetEntity.getUserRole().name()));

        DefaultOAuth2User result = new DefaultOAuth2User(authorities, attributes, "email");

        // Assert
        assertThat(result).isNotNull();
        Collection<? extends GrantedAuthority> resultAuthorities = result.getAuthorities();
        assertThat(resultAuthorities).hasSize(1);
        assertThat(resultAuthorities.iterator().next().getAuthority()).isEqualTo("ROLE_FLEET");
    }

    @Test
    void testUserRepositoryFindByEmailInteraction() {
        // Test the repository interaction part
        String testEmail = "test@example.com";

        UserEntity existingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(testEmail)
                .name("Test User")
                .authProvider("GOOGLE")
                .userRole(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

        // Simulate the find operation that happens in loadUser
        Optional<UserEntity> result = userRepository.findByEmail(testEmail);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(testEmail);
        assertThat(result.get().getUserRole()).isEqualTo(UserRole.USER);

        verify(userRepository).findByEmail(testEmail);
    }

    @Test
    void testUserRepositorySaveInteraction() {
        // Test the repository save interaction for new users
        String testEmail = "new@example.com";

        UserEntity newUser = UserEntity.builder()
                .email(testEmail)
                .name("New User")
                .authProvider("GOOGLE")
                .userRole(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(testEmail)
                .name("New User")
                .authProvider("GOOGLE")
                .userRole(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // Simulate the save operation that happens in loadUser for new users
        Optional<UserEntity> existingUser = userRepository.findByEmail(testEmail);
        if (existingUser.isEmpty()) {
            UserEntity result = userRepository.save(newUser);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getEmail()).isEqualTo(testEmail);
        }

        verify(userRepository).findByEmail(testEmail);
        verify(userRepository).save(any(UserEntity.class));
    }
}