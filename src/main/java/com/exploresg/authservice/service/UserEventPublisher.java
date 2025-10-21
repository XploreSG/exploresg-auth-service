package com.exploresg.authservice.service;

import com.exploresg.authservice.event.UserCreatedEvent;
import com.exploresg.authservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing user-related events to RabbitMQ.
 * Events are consumed by downstream services like the notification service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.routing-key.user-created}")
    private String userCreatedRoutingKey;

    /**
     * Publishes a user created event to RabbitMQ.
     * Retries up to 3 times with 2-second delays if RabbitMQ is temporarily
     * unavailable.
     * The notification service will consume this event and send a welcome email via
     * AWS SES.
     *
     * @param user The newly created user
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000), retryFor = { Exception.class })
    public void publishUserCreatedEvent(User user) {
        try {
            UserCreatedEvent event = UserCreatedEvent.fromUser(user);

            log.info("Publishing UserCreatedEvent for user: {} (userId: {}, email: {})",
                    user.getName(), user.getId(), user.getEmail());

            rabbitTemplate.convertAndSend(
                    userEventsExchange,
                    userCreatedRoutingKey,
                    event);

            log.info("Successfully published UserCreatedEvent for userId: {}", user.getId());

            // Also send welcome email notification to notification service
            sendWelcomeEmailNotification(user);

        } catch (Exception e) {
            // Log the error but don't fail the user creation process
            log.error("Failed to publish UserCreatedEvent for userId: {}. Error: {}",
                    user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Sends welcome email notification directly to notification service's queue.
     * Uses the format expected by the notification service (Spring Cloud Stream).
     */
    private void sendWelcomeEmailNotification(User user) {
        try {
            // Create notification in the format expected by notification service
            var notification = java.util.Map.of(
                    "recipientEmail", user.getEmail(),
                    "recipientName", user.getName() != null ? user.getName() : user.getGivenName(),
                    "emailType", "WELCOME",
                    "templateData", java.util.Map.of(
                            "userName", user.getName() != null ? user.getName() : user.getGivenName()));

            // Send directly to notification service's queue (no exchange/routing needed for
            // Spring Cloud Stream)
            rabbitTemplate.convertAndSend(
                    "sendNotification-in-0", // Notification service's input queue
                    notification);

            log.info("✉️ Welcome email notification sent to notification service for: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send welcome email notification for userId: {}. Error: {}",
                    user.getId(), e.getMessage(), e);
        }
    }
}
