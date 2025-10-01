package com.exploresg.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SignupResponse {
    private Long userId;
    private String email;
    private String givenName;
    private String familyName;
    private String picture;

    private String phone;
    private LocalDate dateOfBirth;
    private String drivingLicenseNumber;
    private String passportNumber;
    private String preferredLanguage;
    private String countryOfResidence;
}
