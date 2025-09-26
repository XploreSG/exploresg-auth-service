package com.exploresg.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=ExplOreSG-2025-Super-Secret-JWT-Signing-Key-For-Authentication-Service-Do-Not-Share-This-Key",
        "jwt.expiration=86400000"
})
class JwtProviderTest {

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void testValidateToken_withNullToken_shouldReturnFalse() {
        // Test null token - should not throw NPE
        boolean result = jwtProvider.validateToken(null);
        assertFalse(result);
    }

    @Test
    void testValidateToken_withEmptyToken_shouldReturnFalse() {
        // Test empty token - should not throw NPE
        boolean result = jwtProvider.validateToken("");
        assertFalse(result);
    }

    @Test
    void testValidateToken_withWhitespaceToken_shouldReturnFalse() {
        // Test whitespace token - should not throw NPE
        boolean result = jwtProvider.validateToken("   ");
        assertFalse(result);
    }

    @Test
    void testValidateToken_withInvalidToken_shouldReturnFalse() {
        // Test invalid token - should not throw exception
        boolean result = jwtProvider.validateToken("invalid-token");
        assertFalse(result);
    }

    @Test
    void testGetUserIdFromToken_withNullToken_shouldReturnNull() {
        // Test null token - should not throw NPE
        String result = jwtProvider.getUserIdFromToken(null);
        assertNull(result);
    }

    @Test
    void testGetEmailFromToken_withEmptyToken_shouldReturnNull() {
        // Test empty token - should not throw NPE
        String result = jwtProvider.getEmailFromToken("");
        assertNull(result);
    }

    @Test
    void testGetRolesFromToken_withNullToken_shouldReturnEmptySet() {
        // Test null token - should not throw NPE
        Set<String> result = jwtProvider.getRolesFromToken(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testIsTokenExpired_withNullToken_shouldReturnTrue() {
        // Test null token - should return true (treat as expired)
        boolean result = jwtProvider.isTokenExpired(null);
        assertTrue(result);
    }

    @Test
    void testValidToken_completeWorkflow() {
        // Test complete workflow with valid token
        String userId = "test-user-123";
        String email = "test@example.com";
        Set<String> roles = Set.of("USER", "ADMIN");

        // Generate token
        String token = jwtProvider.generateToken(userId, email, roles);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validate token
        assertTrue(jwtProvider.validateToken(token));

        // Extract data
        assertEquals(userId, jwtProvider.getUserIdFromToken(token));
        assertEquals(email, jwtProvider.getEmailFromToken(token));
        assertEquals(roles, jwtProvider.getRolesFromToken(token));
        assertFalse(jwtProvider.isTokenExpired(token));
    }
}