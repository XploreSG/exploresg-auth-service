# RabbitMQ Integration Guide

## Overview

The Auth Service is integrated with RabbitMQ to publish user creation events to the Notification Service, which handles sending welcome emails via AWS SES.

## Architecture

```
┌─────────────────┐         ┌──────────────┐         ┌──────────────────────┐
│   Auth Service  │ ------> │   RabbitMQ   │ ------> │ Notification Service │
│                 │ Publish │              │ Consume │                      │
│ (User Created)  │         │   Exchange   │         │  (Send Email via SES)│
└─────────────────┘         └──────────────┘         └──────────────────────┘
```

## Message Flow

1. **User Registration/Creation**: When a new user is created via Google SSO in `UserService.upsertUserFromJwt()`
2. **Event Publishing**: `UserEventPublisher` publishes a `UserCreatedEvent` to RabbitMQ
3. **Message Routing**: RabbitMQ routes the message to `exploresg.user.created` queue
4. **Notification Processing**: Notification Service consumes the event and sends welcome email via AWS SES

## Configuration

### RabbitMQ Settings

The following configuration is added to `application.properties`:

```properties
# RabbitMQ Connection
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
spring.rabbitmq.virtual-host=${RABBITMQ_VIRTUAL_HOST:/}

# Exchange and Queue Configuration
rabbitmq.exchange.user-events=${RABBITMQ_EXCHANGE_USER_EVENTS:exploresg.user.events}
rabbitmq.queue.user-created=${RABBITMQ_QUEUE_USER_CREATED:exploresg.user.created}
rabbitmq.routing-key.user-created=${RABBITMQ_ROUTING_KEY_USER_CREATED:user.created}
```

### Environment Variables

Set these in your `.env` file or environment:

```bash
# RabbitMQ Configuration (uses RabbitMQ from notification service)
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

For production, use secure credentials:

```bash
RABBITMQ_USERNAME=your_secure_username
RABBITMQ_PASSWORD=your_secure_password
```

## Event Schema

### UserCreatedEvent

```json
{
  "userId": 123,
  "userUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe",
  "givenName": "John",
  "familyName": "Doe",
  "identityProvider": "GOOGLE",
  "role": "USER",
  "createdAt": "2025-10-21T10:30:00",
  "eventType": "USER_CREATED",
  "eventTimestamp": "2025-10-21T10:30:00"
}
```

## Components

### 1. RabbitMQConfig (`config/RabbitMQConfig.java`)

Sets up:

- **Exchange**: `exploresg.user.events` (Topic Exchange)
- **Queue**: `exploresg.user.created` (Durable queue with dead-letter support)
- **Binding**: Routes messages with key `user.created` to the queue
- **Message Converter**: Jackson2JsonMessageConverter for JSON serialization

### 2. UserCreatedEvent (`event/UserCreatedEvent.java`)

Event payload containing:

- User identification (ID, UUID, email)
- User details (name, role, identity provider)
- Event metadata (type, timestamp)

### 3. UserEventPublisher (`service/UserEventPublisher.java`)

Responsible for:

- Publishing `UserCreatedEvent` to RabbitMQ
- Error handling (logs failures without breaking user creation)
- Using configured exchange and routing key

### 4. UserService (Updated)

Modified to:

- Inject `UserEventPublisher`
- Publish event after successful user creation
- Only publishes for NEW users (not updates)

## Testing

### 1. Local Development

Ensure RabbitMQ is running (via notification service docker-compose):

```bash
# Start notification service with RabbitMQ
cd ../exploresg-notification-service
docker-compose up -d rabbitmq

# Or start all services
docker-compose up -d
```

### 2. Verify RabbitMQ Connection

Check RabbitMQ Management UI:

- URL: http://localhost:15672
- Default credentials: guest/guest

### 3. Create a Test User

```bash
# Trigger user creation via Google SSO
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+65-9123-4567",
    "dateOfBirth": "1990-01-15"
  }'
```

### 4. Monitor Logs

Auth Service logs:

```bash
docker logs -f dev-exploresg-auth-service
```

Look for:

```
Publishing UserCreatedEvent for user: John Doe (userId: 123, email: user@example.com)
Successfully published UserCreatedEvent for userId: 123
```

### 5. Check RabbitMQ Queue

In RabbitMQ Management UI:

- Navigate to Queues → `exploresg.user.created`
- Check message count (should increase when users are created)
- View messages to inspect payload

## Error Handling

### Publishing Failures

The `UserEventPublisher` catches all exceptions to prevent user creation from failing:

```java
try {
    // Publish event
} catch (Exception e) {
    // Log error but don't fail user creation
    log.error("Failed to publish UserCreatedEvent for userId: {}", user.getId(), e);
}
```

### Dead Letter Queue

Messages that fail processing are routed to:

- Dead Letter Exchange: `exploresg.user.events.dlx`
- Can be configured for retry or manual inspection

## Network Configuration

### Docker Compose Setup

The auth service connects to RabbitMQ via the shared `exploresg-network`:

```yaml
networks:
  - default # For database connection
  - exploresg-network # For RabbitMQ and other services
```

RabbitMQ host is set to `rabbitmq` (container name in notification service).

## Monitoring

### Metrics

The following should be monitored:

- Message publish rate
- Message publish failures
- Queue depth
- Consumer lag (in notification service)

### Health Checks

RabbitMQ health is included in Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/health
```

Response includes RabbitMQ status:

```json
{
  "status": "UP",
  "components": {
    "rabbit": {
      "status": "UP",
      "details": {
        "version": "3.x.x"
      }
    }
  }
}
```

## Integration with Notification Service

### Notification Service Requirements

The notification service should:

1. **Consume from Queue**: `exploresg.user.created`
2. **Process Event**: Extract user details from `UserCreatedEvent`
3. **Send Email**: Use AWS SES to send welcome email
4. **Acknowledge**: Acknowledge message after successful processing

### Sample Consumer (Notification Service)

```java
@RabbitListener(queues = "${rabbitmq.queue.user-created}")
public void handleUserCreated(UserCreatedEvent event) {
    log.info("Received user created event for: {}", event.getEmail());

    // Send welcome email via AWS SES
    emailService.sendWelcomeEmail(
        event.getEmail(),
        event.getGivenName(),
        event.getName()
    );
}
```

## Production Considerations

### 1. Connection Pooling

Configure connection pool for high throughput:

```properties
spring.rabbitmq.cache.connection.mode=CONNECTION
spring.rabbitmq.cache.connection.size=25
```

### 2. Publisher Confirms

Enable publisher confirms for reliability:

```properties
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
```

### 3. Message Persistence

Messages are durable by default (queue configured with `durable=true`).

### 4. Security

- Use TLS for RabbitMQ connections in production
- Implement proper authentication and authorization
- Rotate credentials regularly

### 5. Monitoring

Integrate with:

- Prometheus/Grafana for metrics
- ELK stack for log aggregation
- CloudWatch for AWS deployments

## Troubleshooting

### Issue: Connection Refused

**Symptom**: `java.net.ConnectException: Connection refused`

**Solution**:

1. Verify RabbitMQ is running: `docker ps | grep rabbitmq`
2. Check network connectivity: `docker network ls`
3. Verify `RABBITMQ_HOST` environment variable

### Issue: Authentication Failed

**Symptom**: `com.rabbitmq.client.AuthenticationFailureException`

**Solution**:

1. Verify credentials in `.env` file
2. Check RabbitMQ user permissions
3. Ensure virtual host is correct

### Issue: Messages Not Being Consumed

**Symptom**: Messages accumulate in queue

**Solution**:

1. Check notification service is running
2. Verify consumer is listening to correct queue
3. Check notification service logs for errors

## Related Documentation

- [RabbitMQ Official Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Documentation](https://docs.spring.io/spring-amqp/reference/)
- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)

## Future Enhancements

1. **Event Versioning**: Add schema versioning for backward compatibility
2. **Retry Logic**: Implement exponential backoff for failed publishes
3. **Message Priority**: Prioritize certain user types (e.g., premium users)
4. **Event Enrichment**: Add additional context (user preferences, locale)
5. **Circuit Breaker**: Implement circuit breaker pattern for RabbitMQ failures
