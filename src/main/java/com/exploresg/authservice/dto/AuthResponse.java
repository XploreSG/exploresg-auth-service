package com.exploresg.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private boolean requiresProfileSetup;
    private UserInfo userInfo; // <-- ADD THIS FIELD

    // Add a nested UserInfo DTO to structure the response
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String email;
        private String givenName;
        private String familyName;
        private String picture;
        private String phone;
        private String dateOfBirth;
        private String drivingLicenseNumber;
        private String passportNumber;
        private String preferredLanguage;
        private String countryOfResidence;
    }
}