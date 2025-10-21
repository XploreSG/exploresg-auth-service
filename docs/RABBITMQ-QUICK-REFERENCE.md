# RabbitMQ Integration - Quick Reference

## 🚀 Quick Start

### Environment Variables

```bash
# .env file
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### Start Services

```bash
# Ensure RabbitMQ is running (from notification service)
cd ../exploresg-notification-service
docker-compose up -d rabbitmq

# Start auth service
cd ../exploresg-auth-service
docker-compose up -d
```

## 📡 Message Flow

```
User Registration → UserService → UserEventPublisher → RabbitMQ → Notification Service → AWS SES
```

## 🔧 Key Components

| Component            | Purpose                                 |
| -------------------- | --------------------------------------- |
| `RabbitMQConfig`     | Configure exchange, queue, and bindings |
| `UserCreatedEvent`   | Event payload schema                    |
| `UserEventPublisher` | Publish events to RabbitMQ              |
| `UserService`        | Trigger event on new user creation      |

## 📝 Event Schema

```json
{
  "userId": 123,
  "userUuid": "uuid-here",
  "email": "user@example.com",
  "name": "John Doe",
  "identityProvider": "GOOGLE",
  "role": "USER",
  "eventType": "USER_CREATED"
}
```

## 🔍 Testing Commands

### Check RabbitMQ Status

```bash
docker exec -it rabbitmq rabbitmq-diagnostics status
```

### View Logs

```bash
docker logs -f dev-exploresg-auth-service | grep "UserCreatedEvent"
```

### Access Management UI

```
http://localhost:15672
Credentials: guest/guest
```

### Monitor Queue

```bash
# Queue: exploresg.user.created
# Exchange: exploresg.user.events
# Routing Key: user.created
```

## 🐛 Troubleshooting

| Issue                 | Solution                                                |
| --------------------- | ------------------------------------------------------- |
| Connection refused    | Check RabbitMQ is running: `docker ps \| grep rabbitmq` |
| Auth failure          | Verify credentials in `.env`                            |
| Messages not consumed | Check notification service is running and listening     |

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
# Look for "rabbit": { "status": "UP" }
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics/rabbitmq.published
```

## 🔐 Production Checklist

- [ ] Use secure RabbitMQ credentials
- [ ] Enable TLS for RabbitMQ connections
- [ ] Configure dead-letter queues
- [ ] Set up monitoring and alerting
- [ ] Implement message retry logic
- [ ] Configure connection pooling
- [ ] Enable publisher confirms

## 📚 Configuration Reference

### Exchange

- **Name**: `exploresg.user.events`
- **Type**: Topic
- **Durable**: Yes

### Queue

- **Name**: `exploresg.user.created`
- **Durable**: Yes
- **DLX**: `exploresg.user.events.dlx`

### Routing Key

- **Pattern**: `user.created`

## 🔗 Related Docs

- [Full Integration Guide](./RABBITMQ-INTEGRATION.md)
- [Testing Guide](./INTEGRATION-TESTING-RABBITMQ.md)
