package com.exploresg.authservice.dto;

import lombok.Data;

import java.time.LocalDate;

import com.exploresg.authservice.model.Role;

@Data
public class SignupProfileRequest {

    // optional overrides
    private String givenName;
    private String familyName;

    // Profile fields - optional at signup, can be collected later (e.g., before
    // booking)
    private String phone;
    private LocalDate dateOfBirth;
    private String drivingLicenseNumber;

    private String passportNumber;
    private String preferredLanguage;
    private String countryOfResidence;

    private Role requestedRole;
}
