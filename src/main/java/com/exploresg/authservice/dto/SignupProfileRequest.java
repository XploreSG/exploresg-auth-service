package com.exploresg.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignupProfileRequest {

    // optional overrides
    private String givenName;
    private String familyName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Driving license number is required")
    private String drivingLicenseNumber;

    private String passportNumber;
    private String preferredLanguage;
    private String countryOfResidence;
}
