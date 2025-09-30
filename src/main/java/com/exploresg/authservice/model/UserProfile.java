package com.exploresg.authservice.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private Long id; // same as app_user.id

    @OneToOne
    @MapsId // ensures profile.id = user.id
    @JoinColumn(name = "id")
    private User user;

    private String phone;

    private LocalDate dateOfBirth;

    private String drivingLicenseNumber;

    private String passportNumber; // optional, for tourists only

    private String preferredLanguage;

    private String countryOfResidence;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
