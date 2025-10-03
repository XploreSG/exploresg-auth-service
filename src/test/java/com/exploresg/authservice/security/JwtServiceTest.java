package com.exploresg.authservice.security;

import com.exploresg.authservice.model.Role;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.model.IdentityProvider;
import com.exploresg.authservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("jane.doe@example.com")
                .givenName("Jane")
                .familyName("Doe")
                .picture("https://example.com/jane.png")
                .role(Role.USER)
                .identityProvider(IdentityProvider.GOOGLE)
                .isActive(true)
                .build();
    }

    @Test
    void generate_and_validate_token_contains_custom_claims() {
        // generate a token with explicit extra claims
        Map<String, Object> extra = new HashMap<>();
        extra.put("custom", "val");

        String token = jwtService.generateToken(extra, (UserDetails) testUser);
        assertThat(token).isNotNull().isNotEmpty();

        // username should be present
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo(testUser.getUsername());

        // token must be valid for the same user
        assertThat(jwtService.isTokenValid(token, (UserDetails) testUser)).isTrue();

        // custom claims added by JwtService generateToken (roles, givenName,
        // familyName, picture)
        String givenName = (String) jwtService.extractClaim(token, claims -> claims.get("givenName"));
        String familyName = (String) jwtService.extractClaim(token, claims -> claims.get("familyName"));
        String picture = (String) jwtService.extractClaim(token, claims -> claims.get("picture"));

        assertThat(givenName).isEqualTo(testUser.getGivenName());
        assertThat(familyName).isEqualTo(testUser.getFamilyName());
        assertThat(picture).isEqualTo(testUser.getPicture());
    }

}
