package com.exploresg.authservice.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "auth_provider", nullable = false, length = 50)
    @Builder.Default
    private String authProvider = "GOOGLE";

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    @Builder.Default
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    @Builder.Default
    private UserRole userRole = UserRole.USER;

    @Column(length = 255)
    private String name;

    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Manual getters to bypass Lombok issues
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Manual setters
    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Manual builder method
    public static UserEntityBuilder builder() {
        return new UserEntityBuilder();
    }

    public static class UserEntityBuilder {
        private UUID id;
        private String email;
        private String authProvider;
        private UserStatus userStatus = UserStatus.ACTIVE;
        private UserRole userRole = UserRole.USER;
        private String name;
        private String pictureUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserEntityBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UserEntityBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserEntityBuilder authProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public UserEntityBuilder userStatus(UserStatus userStatus) {
            this.userStatus = userStatus;
            return this;
        }

        public UserEntityBuilder userRole(UserRole userRole) {
            this.userRole = userRole;
            return this;
        }

        public UserEntityBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserEntityBuilder pictureUrl(String pictureUrl) {
            this.pictureUrl = pictureUrl;
            return this;
        }

        public UserEntityBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserEntityBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserEntity build() {
            UserEntity entity = new UserEntity();
            entity.setId(id);
            entity.setEmail(email);
            entity.setAuthProvider(authProvider);
            entity.setUserStatus(userStatus);
            entity.setUserRole(userRole);
            entity.setName(name);
            entity.setPictureUrl(pictureUrl);
            entity.setCreatedAt(createdAt);
            entity.setUpdatedAt(updatedAt);
            return entity;
        }
    }
}
