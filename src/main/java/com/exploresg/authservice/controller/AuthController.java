package com.exploresg.authservice.controller;

import com.exploresg.authservice.dto.SignupProfileRequest;
import com.exploresg.authservice.dto.SignupResponse;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.model.UserProfile;
import com.exploresg.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // This endpoint remains public for pre-login checks if needed
    @GetMapping("/check")
    public ResponseEntity<?> checkUser(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email parameter is required"));
        }
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists, "email", email));
    }

    // This secured endpoint returns details for the currently logged-in user
    @GetMapping("/me")
    public ResponseEntity<User> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    // This is the corrected signup method
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signupProfile(
            @AuthenticationPrincipal User user, // <-- FIX: Correctly expects a 'User' object
            @Valid @RequestBody SignupProfileRequest request) {

        log.info("User signup/profile update initiated for userId: {}, email: {}",
                user.getId(), user.getEmail());

        UserProfile profile = userService.createOrUpdateProfile(user.getId(), request);

        log.info("User profile successfully created/updated for userId: {}", user.getId());

        SignupResponse response = new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getGivenName(),
                user.getFamilyName(),
                user.getPicture(),
                profile.getPhone(),
                profile.getDateOfBirth(),
                profile.getDrivingLicenseNumber(),
                profile.getPassportNumber(),
                profile.getPreferredLanguage(),
                profile.getCountryOfResidence());

        return ResponseEntity.ok(response);
    }

    // Role-protected endpoints
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAdminDashboard(@AuthenticationPrincipal User user) {
        log.info("Admin dashboard accessed by userId: {}, email: {}",
                user.getId(), user.getEmail());
        return ResponseEntity.ok("Welcome to the Admin Dashboard!");
    }

    @GetMapping("/fleet/vehicles")
    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'ADMIN')")
    public ResponseEntity<String> getFleetVehicles(@AuthenticationPrincipal User user) {
        log.info("Fleet vehicles accessed by userId: {}, role: {}",
                user.getId(), user.getRole());
        return ResponseEntity.ok("Here is the list of fleet vehicles.");
    }
}