package com.exploresg.authservice.model;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserModelTest {

    @Test
    void user_getAuthorities_and_getUsername() {
        User u = User.builder()
                .id(2L)
                .email("bob@example.com")
                .role(Role.ADMIN)
                .identityProvider(IdentityProvider.LOCAL)
                .isActive(true)
                .build();

        Collection<?> authorities = u.getAuthorities();
        assertThat(authorities).isNotEmpty();
        assertThat(u.getUsername()).isEqualTo("bob@example.com");
        // role name should be reflected as a ROLE_<NAME> authority
        assertThat(authorities.iterator().next().toString()).contains("ROLE_ADMIN");
    }

}
