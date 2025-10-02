package com.exploresg.authservice.dto;

import com.exploresg.authservice.model.Role;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthSessionResponse {
    private Long userId;
    private String email;
    private String givenName;
    private String familyName;
    private String picture;
    private Role role;
    private TokenPairResponse tokens;
}
