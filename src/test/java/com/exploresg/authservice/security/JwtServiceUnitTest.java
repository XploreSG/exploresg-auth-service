package com.exploresg.authservice.security;

import com.exploresg.authservice.model.IdentityProvider;
import com.exploresg.authservice.model.Role;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceUnitTest {

    // reuse same secret used in application/test resources
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = JwtService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void generateToken_and_extract_claims_without_spring_context() throws Exception {
        JwtService svc = new JwtService();
        // configure fields normally provided by Spring
        setField(svc, "secretKey", TEST_SECRET);
        setField(svc, "jwtExpiration", 86400000L);
        setField(svc, "refreshExpiration", 604800000L);

        User u = User.builder()
                .id(10L)
                .email("alice@example.com")
                .givenName("Alice")
                .familyName("Wonder")
                .picture("https://example.com/alice.jpg")
                .role(Role.USER)
                .identityProvider(IdentityProvider.LOCAL)
                .isActive(true)
                .build();

        String token = svc.generateToken(u);
        assertThat(token).isNotNull().isNotEmpty();

        String username = svc.extractUsername(token);
        assertThat(username).isEqualTo(u.getUsername());

        // verify claims exist and map to user fields
        String extractedGiven = (String) svc.extractClaim(token, claims -> claims.get("givenName"));
        assertThat(extractedGiven).isEqualTo(u.getGivenName());

        Date exp = svc.extractClaim(token, Claims::getExpiration);
        assertThat(exp).isAfter(new Date());
    }

    @Test
    void expired_token_is_invalid() throws Exception {
        JwtService svc = new JwtService();
        setField(svc, "secretKey", TEST_SECRET);
        // set expiration to negative to force expired token
        setField(svc, "jwtExpiration", -1000L);
        setField(svc, "refreshExpiration", 1000L);

        User u = User.builder().id(11L).email("old@example.com").role(Role.USER)
                .identityProvider(IdentityProvider.LOCAL).build();
        String token = svc.generateToken(u);
        try {
            boolean valid = svc.isTokenValid(token, u);
            // If no exception, token should be reported invalid
            assertThat(valid).isFalse();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Parsing may throw ExpiredJwtException for expired tokens; treat as expected
            // Test passes if exception is thrown (token is expired)
        }
    }

    @Test
    void refresh_token_expiration_is_greater_than_access_token() throws Exception {
        JwtService svc = new JwtService();
        setField(svc, "secretKey", TEST_SECRET);
        setField(svc, "jwtExpiration", 1000L); // 1s
        setField(svc, "refreshExpiration", 5000L); // 5s

        User u = User.builder().id(12L).email("refresh@example.com").role(Role.USER)
                .identityProvider(IdentityProvider.LOCAL).build();

        String access = svc.generateToken(u);
        String refresh = svc.generateRefreshToken(u);

        Date accessExp = svc.extractClaim(access, Claims::getExpiration);
        Date refreshExp = svc.extractClaim(refresh, Claims::getExpiration);

        assertThat(refreshExp.getTime()).isGreaterThan(accessExp.getTime());
    }

}
