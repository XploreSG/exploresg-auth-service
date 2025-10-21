# RabbitMQ Integration Guide for Auth Service

This guide explains how to integrate your service with the ExploreSG Notification Service via RabbitMQ.

## Prerequisites

- Spring Boot application
- Access to the `exploresg-net` Docker network
- RabbitMQ connection details (host: `rabbitmq`, port: `5672`)

---

## 1. Docker Compose Configuration

Add the shared network to your `docker-compose.yaml`:

```yaml
services:
  your-auth-service:
    # ... your existing configuration
    environment:
      RABBIT_HOST: ${RABBIT_HOST:-rabbitmq}
      RABBIT_USERNAME: ${RABBIT_USERNAME:-guest}
      RABBIT_PASSWORD: ${RABBIT_PASSWORD:-guest}
    networks:
      - exploresg-network

networks:
  exploresg-network:
    name: exploresg-net
    external: true
```

---

## 2. Maven Dependencies

Add these dependencies to your `pom.xml`:

```xml
<!-- Spring Cloud Stream -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream</artifactId>
</dependency>

<!-- RabbitMQ Binder -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-rabbit</artifactId>
</dependency>
```

Add Spring Cloud BOM to your `dependencyManagement` section:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2024.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 3. Application Configuration

Add to your `application.properties`:

```properties
# RabbitMQ Connection
spring.rabbitmq.host=${RABBIT_HOST:rabbitmq}
spring.rabbitmq.port=5672
spring.rabbitmq.username=${RABBIT_USERNAME:guest}
spring.rabbitmq.password=${RABBIT_PASSWORD:guest}

# Spring Cloud Stream Bindings
spring.cloud.stream.bindings.sendNotification-out-0.destination=sendNotification-in-0
spring.cloud.stream.bindings.sendNotification-out-0.content-type=application/json
spring.cloud.function.definition=
```

---

## 4. Create Email Notification DTO

```java
package com.yourapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationMessage {
    private String recipientEmail;
    private String recipientName;
    private String emailType;  // "WELCOME", "PASSWORD_RESET", "ACCOUNT_VERIFICATION", etc.
    private Map<String, Object> templateData;
}
```

---

## 5. Create Notification Publisher Service

```java
package com.yourapp.service;

import com.yourapp.dto.EmailNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final StreamBridge streamBridge;
    private static final String OUTPUT_CHANNEL = "sendNotification-out-0";

    /**
     * Send an email notification via RabbitMQ
     */
    public void sendEmailNotification(EmailNotificationMessage message) {
        try {
            boolean sent = streamBridge.send(OUTPUT_CHANNEL, message);
            if (sent) {
                log.info("Email notification sent to queue for: {}", message.getRecipientEmail());
            } else {
                log.error("Failed to send email notification to queue for: {}", message.getRecipientEmail());
            }
        } catch (Exception e) {
            log.error("Error sending email notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send welcome email after user registration
     */
    public void sendWelcomeEmail(String userEmail, String userName) {
        EmailNotificationMessage message = new EmailNotificationMessage();
        message.setRecipientEmail(userEmail);
        message.setRecipientName(userName);
        message.setEmailType("WELCOME");
        message.setTemplateData(Map.of("userName", userName));

        sendEmailNotification(message);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String userEmail, String userName, String resetToken) {
        EmailNotificationMessage message = new EmailNotificationMessage();
        message.setRecipientEmail(userEmail);
        message.setRecipientName(userName);
        message.setEmailType("PASSWORD_RESET");
        message.setTemplateData(Map.of(
            "userName", userName,
            "resetToken", resetToken,
            "resetLink", "https://xplore.town/reset-password?token=" + resetToken
        ));

        sendEmailNotification(message);
    }

    /**
     * Send account verification email
     */
    public void sendVerificationEmail(String userEmail, String userName, String verificationToken) {
        EmailNotificationMessage message = new EmailNotificationMessage();
        message.setRecipientEmail(userEmail);
        message.setRecipientName(userName);
        message.setEmailType("ACCOUNT_VERIFICATION");
        message.setTemplateData(Map.of(
            "userName", userName,
            "verificationToken", verificationToken,
            "verificationLink", "https://xplore.town/verify?token=" + verificationToken
        ));

        sendEmailNotification(message);
    }
}
```

---

## 6. Usage in Your Controllers/Services

### Example: User Registration

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final NotificationPublisher notificationPublisher;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        // Create user
        User user = userService.createUser(request);

        // Send welcome email asynchronously via RabbitMQ
        notificationPublisher.sendWelcomeEmail(user.getEmail(), user.getUsername());

        return ResponseEntity.ok(new UserResponse(user));
    }
}
```

### Example: Forgot Password

```java
@PostMapping("/forgot-password")
public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    // Find user and generate reset token
    User user = userService.findByEmail(request.getEmail());
    String resetToken = userService.generatePasswordResetToken(user);

    // Send password reset email
    notificationPublisher.sendPasswordResetEmail(
        user.getEmail(),
        user.getUsername(),
        resetToken
    );

    return ResponseEntity.ok(new MessageResponse("Password reset email sent"));
}
```

---

## 7. Available Email Types

The Notification Service currently supports these email types:

| Email Type             | Description                 | Required Template Data                              |
| ---------------------- | --------------------------- | --------------------------------------------------- |
| `WELCOME`              | Welcome email for new users | `userName`                                          |
| `BOOKING_CONFIRMATION` | Booking confirmation email  | TBD                                                 |
| `BOOKING_CANCELLATION` | Booking cancellation email  | TBD                                                 |
| `PASSWORD_RESET`       | Password reset email        | `userName`, `resetToken`, `resetLink`               |
| `ACCOUNT_VERIFICATION` | Account verification email  | `userName`, `verificationToken`, `verificationLink` |
| `PROMOTIONAL`          | Promotional/marketing email | TBD                                                 |
| `REMINDER`             | Reminder email              | TBD                                                 |

---

## 8. Testing the Integration

### Check if your service connects to RabbitMQ

Look for this in your application logs:

```
Created new connection: rabbitConnectionFactory#xxx
```

### Verify message delivery

1. **RabbitMQ Management UI**: http://localhost:15672

   - Username: `guest`
   - Password: `guest`
   - Check the `sendNotification-in-0` queue for messages

2. **Notification Service Logs**:

   ```bash
   docker logs dev-exploresg-notification-service
   ```

   Look for: `Processing email request for: <email>`

3. **Check your email inbox** for the delivered email

---

## 9. Important Notes

- ✅ **Network**: Services must be on the `exploresg-net` Docker network
- ✅ **Async Processing**: Emails are sent asynchronously - no HTTP response delay
- ✅ **Error Handling**: The notification service handles email delivery failures
- ✅ **Retry Logic**: Failed messages can be configured to retry
- ✅ **Channel Name**: Always use `sendNotification-out-0` as the output channel
- ✅ **RabbitMQ Host**: Use `rabbitmq` (not `localhost`) in Docker environment

---

## 10. Troubleshooting

### Service can't connect to RabbitMQ

- Verify you're on the `exploresg-net` network
- Check RabbitMQ container is running: `docker ps | grep rabbitmq`
- Verify credentials match: `guest/guest`

### Messages not being consumed

- Check notification service logs for errors
- Verify the queue name is `sendNotification-in-0`
- Check RabbitMQ UI for unacknowledged messages

### Emails not arriving

- Check notification service logs for AWS SES errors
- Verify recipient email is verified in AWS SES (if in Sandbox mode)
- Check spam/junk folders

---

## Contact & Support

For issues or questions about the notification service integration:

- Check the notification service repository
- Review RabbitMQ logs: `docker logs rabbitmq`
- Review notification service logs: `docker logs dev-exploresg-notification-service`

---

**Last Updated**: October 21, 2025
