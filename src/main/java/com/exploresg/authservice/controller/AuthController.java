package com.exploresg.authservice.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exploresg.authservice.dto.SignupProfileRequest;
import com.exploresg.authservice.dto.SignupResponse;
import com.exploresg.authservice.model.User;
import com.exploresg.authservice.model.UserProfile;
import com.exploresg.authservice.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/check")
    public ResponseEntity<?> checkUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = jwt.getClaim("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No email in token"));
        }

        boolean exists = userService.existsByEmail(email);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "email", email));
    }

    @PostMapping("/auth/log-token")
    public ResponseEntity<?> logToken(@RequestHeader("Authorization") String authHeader) {
        // Remove "Bearer " prefix if present
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        System.out.println("Received token: " + token);
        return ResponseEntity.ok("Token logged successfully");
    }

    // Secured endpoint for any authenticated user
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        // For /me we just fetch or create the user, no role assignment
        User user = userService.upsertUserFromJwt(jwt, null);
        return ResponseEntity.ok(user);
    }

    // Secured endpoint for Admins only
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAdminDashboard() {
        return ResponseEntity.ok("Welcome to the Admin Dashboard!");
    }

    // Secured endpoint for Fleet Managers and Admins
    @GetMapping("/fleet/vehicles")
    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'ADMIN')")
    public ResponseEntity<String> getFleetVehicles() {
        return ResponseEntity.ok("Here is the list of fleet vehicles.");
    }

    // Existing signup logic
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signupProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SignupProfileRequest request) {

        // Pass requestedRole from the signup request (only applied if user is new)
        User user = userService.upsertUserFromJwt(jwt, request.getRequestedRole());

        UserProfile profile = userService.createOrUpdateProfile(user.getId(), request);

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

}
