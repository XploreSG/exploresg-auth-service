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
            // Create simplified message with just the essential fields
            var userCreatedMessage = java.util.Map.of(
                    "email", user.getEmail(),
                    "name", user.getName() != null ? user.getName() : user.getGivenName(),
                    "givenName", user.getGivenName() != null ? user.getGivenName() : "");

            log.info("Publishing UserCreatedEvent for user: {} (email: {})",
                    userCreatedMessage.get("name"), user.getEmail());

            // Publish to exchange with empty routing key (as expected by notification
            // service)
            rabbitTemplate.convertAndSend(
                    userEventsExchange,
                    "", // Empty routing key for fanout-style delivery
                    userCreatedMessage);

            log.info("✅ Successfully published UserCreatedEvent to exchange '{}' for: {}",
                    userEventsExchange, user.getEmail());
        } catch (Exception e) {
            // Log the error but don't fail the user creation process
            log.error("Failed to publish UserCreatedEvent for user: {}. Error: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

}
