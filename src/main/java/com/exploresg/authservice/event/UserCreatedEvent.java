package com.exploresg.authservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event payload sent to RabbitMQ when a new user is created.
 * This event is consumed by the notification service to send welcome emails via
 * AWS SES.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {

    private Long userId;
    private UUID userUuid;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private String identityProvider;
    private String role;
    private LocalDateTime createdAt;

    // Additional context for notification service
    private String eventType;
    private LocalDateTime eventTimestamp;

    /**
     * Factory method to create event from User entity
     */
    public static UserCreatedEvent fromUser(com.exploresg.authservice.model.User user) {
        return UserCreatedEvent.builder()
                .userId(user.getId())
                .userUuid(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .givenName(user.getGivenName())
                .familyName(user.getFamilyName())
                .identityProvider(user.getIdentityProvider().name())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .eventType("USER_CREATED")
                .eventTimestamp(LocalDateTime.now())
                .build();
    }
}
