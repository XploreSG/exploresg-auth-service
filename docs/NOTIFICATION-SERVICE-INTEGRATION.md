# Notification Service Integration - Welcome Email

## Overview

The Auth Service publishes user creation events to RabbitMQ. The Notification Service should consume these messages and send welcome emails via AWS SES.

---

## Message Queue

**Queue Name:** `sendNotification-in-0`

**Message Format:** JSON

---

## Message Schema

```json
{
  "recipientEmail": "user@example.com",
  "recipientName": "John Doe",
  "emailType": "WELCOME",
  "templateData": {
    "userName": "John Doe"
  }
}
```

### Field Descriptions

| Field                   | Type   | Description                           |
| ----------------------- | ------ | ------------------------------------- |
| `recipientEmail`        | String | User's email address (required)       |
| `recipientName`         | String | User's full name (required)           |
| `emailType`             | String | Always "WELCOME" for new user emails  |
| `templateData.userName` | String | User's name for email personalization |

---

## When Messages Are Published

Messages are published when:

- ✅ A new user logs in via **Google SSO for the first time**
- ✅ User is successfully created in the database
- ✅ Auth service successfully connects to RabbitMQ

Messages are NOT published when:

- ❌ Existing user logs in again
- ❌ User creation fails
- ❌ RabbitMQ connection fails after 3 retry attempts

---

## Consumer Implementation

### Option 1: Spring Cloud Stream (Recommended)

```java
@Service
@Slf4j
public class WelcomeEmailConsumer {

    @Autowired
    private EmailService emailService;

    @Bean
    public Consumer<EmailNotification> handleWelcomeEmail() {
        return notification -> {
            try {
                log.info("Received welcome email request for: {}",
                        notification.getRecipientEmail());

                if ("WELCOME".equals(notification.getEmailType())) {
                    emailService.sendWelcomeEmail(
                        notification.getRecipientEmail(),
                        notification.getRecipientName()
                    );
                    log.info("Welcome email sent successfully to: {}",
                            notification.getRecipientEmail());
                }
            } catch (Exception e) {
                log.error("Failed to send welcome email: {}", e.getMessage(), e);
                throw e; // Requeue for retry
            }
        };
    }
}

// DTO Class
@Data
public class EmailNotification {
    private String recipientEmail;
    private String recipientName;
    private String emailType;
    private Map<String, Object> templateData;
}
```

### Configuration

Add to `application.properties`:

```properties
# Spring Cloud Stream configuration (already exists)
spring.cloud.function.definition=handleWelcomeEmail
spring.cloud.stream.bindings.handleWelcomeEmail-in-0.destination=sendNotification-in-0
spring.cloud.stream.bindings.handleWelcomeEmail-in-0.group=notification-service
```

---

## Message Guarantees

### Reliability

- ✅ **Retry on Publisher Side:** Auth service retries 3 times (2-second delays)
- ✅ **Durable Queue:** Messages survive RabbitMQ restarts
- ✅ **TTL:** Messages expire after 24 hours if not consumed
- ⚠️ **At-Least-Once Delivery:** Same message might be delivered multiple times (idempotency recommended)

### Error Handling

**If notification service fails to process:**

1. Throw exception to reject message
2. RabbitMQ requeues the message
3. Message retried based on your retry configuration
4. After max retries → moves to Dead Letter Queue (if configured)

---

## Testing

### 1. Manual Test - Send Test Message

```bash
# From auth service
curl http://localhost:8080/api/v1/test/rabbitmq/hello
```

### 2. Check Queue

RabbitMQ Management UI: http://localhost:15672

- Login: `guest` / `guest`
- Go to **Queues** → `sendNotification-in-0`
- View message count

### 3. Verify Message Format

In RabbitMQ UI:

1. Click on `sendNotification-in-0`
2. **Get messages** section
3. Click **Get Message(s)**
4. Verify JSON structure

### 4. End-to-End Test

```bash
# 1. Create a new user via Google SSO (first time login)
# 2. Check notification service logs:
docker logs -f dev-exploresg-notification-service

# Expected output:
# Received welcome email request for: user@example.com
# Welcome email sent successfully to: user@example.com
```

---

## Sample Welcome Email Template

```html
Subject: Welcome to ExploreSG! 🎉 Hi {{userName}}, Welcome to ExploreSG -
Singapore's premier car rental platform! We're excited to have you on board. You
can now: ✓ Browse our fleet of vehicles ✓ Book your perfect ride ✓ Manage your
reservations Get started: https://xplore.town Safe travels, The ExploreSG Team
```

---

## Monitoring

### Metrics to Track

1. **Message Processing Rate**

   - Messages consumed per minute
   - Average processing time

2. **Success/Failure Rate**

   - Successful email sends
   - Failed email sends

3. **Queue Depth**
   - Number of pending messages
   - Alert if > 100 messages

### Health Check

Add to your health endpoint:

```java
@Component
public class RabbitMQHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check RabbitMQ connection
        // Check queue depth
        // Return UP or DOWN
    }
}
```

---

## Troubleshooting

### No Messages in Queue

**Check:**

1. Is auth service running? `docker ps | grep auth-service`
2. Is RabbitMQ running? `docker ps | grep rabbitmq`
3. Are services on same network? `docker network inspect exploresg-net`
4. Check auth service logs: `docker logs dev-exploresg-auth-service | grep "Welcome email"`

### Messages Stuck in Queue

**Possible causes:**

1. Notification service not running
2. Consumer not configured correctly
3. Consumer throwing exceptions (check logs)

**Solution:**

```bash
# Check notification service logs
docker logs dev-exploresg-notification-service

# Restart notification service
docker restart dev-exploresg-notification-service
```

### Duplicate Emails Sent

**Cause:** Message processed multiple times

**Solution:** Implement idempotency

```java
// Track sent emails in cache/database
if (emailAlreadySent(userId, "WELCOME")) {
    log.warn("Welcome email already sent to userId: {}", userId);
    return; // Skip sending
}
```

---

## Network Configuration

Both services must be on `exploresg-net` network:

```yaml
# docker-compose.yml
services:
  notification-service:
    networks:
      - exploresg-network

networks:
  exploresg-network:
    name: exploresg-net
    external: true
```

---

## Additional Email Types (Future)

The same queue can handle other email types:

```json
{
  "emailType": "PASSWORD_RESET",
  "recipientEmail": "user@example.com",
  "templateData": {
    "resetLink": "https://xplore.town/reset?token=xyz"
  }
}
```

```json
{
  "emailType": "BOOKING_CONFIRMATION",
  "recipientEmail": "user@example.com",
  "templateData": {
    "bookingId": "BK-12345",
    "vehicleName": "Toyota Camry",
    "pickupDate": "2025-10-25"
  }
}
```

---

## Contact

For questions about message format or integration:

- Check Auth Service docs: `/docs/RABBITMQ-INTEGRATION.md`
- Review test endpoints: `/api/v1/test/rabbitmq/*`

---

**Last Updated:** October 21, 2025
