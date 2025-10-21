# RabbitMQ Integration Testing - Quick Start

## 🚀 Quick Test Commands

### 1. Test RabbitMQ Connection (Hello World)

```bash
curl http://localhost:8080/api/v1/test/rabbitmq/hello
```

**Expected Response:**

```json
{
  "status": "success",
  "message": "Test message sent to RabbitMQ",
  "exchange": "exploresg.user.events",
  "routingKey": "user.created",
  "testEmail": "test@exploresg.com",
  "timestamp": "2025-10-21T10:30:00",
  "instructions": "Check RabbitMQ UI at http://localhost:15672 (guest/guest) or notification service logs"
}
```

### 2. Test UserEventPublisher Service

```bash
curl http://localhost:8080/api/v1/test/rabbitmq/publish
```

### 3. Check RabbitMQ Connection Status

```bash
curl http://localhost:8080/api/v1/test/rabbitmq/status
```

**Expected Response:**

```json
{
  "status": "connected",
  "message": "RabbitMQ connection is active",
  "host": "rabbitmq",
  "port": 5672,
  "virtualHost": "/"
}
```

### 4. Send Custom Test Message

```bash
curl -X POST http://localhost:8080/api/v1/test/rabbitmq/custom \
  -H "Content-Type: application/json" \
  -d '{
    "email": "mytest@example.com",
    "name": "My Test User"
  }'
```

---

## 📋 Step-by-Step Testing Guide

### Step 1: Ensure Services are Running

```bash
# Check RabbitMQ (from notification service)
docker ps | grep rabbitmq

# Check auth service
docker ps | grep auth-service

# Or start everything
docker-compose up -d
```

### Step 2: Test the Connection

```bash
curl http://localhost:8080/api/v1/test/rabbitmq/hello
```

### Step 3: Verify in RabbitMQ UI

1. Open http://localhost:15672
2. Login: `guest` / `guest`
3. Go to **Queues** tab
4. Click on `exploresg.user.created`
5. You should see message count increase

### Step 4: Check Notification Service Logs

```bash
docker logs -f dev-exploresg-notification-service
```

Look for:

```
Processing email request for: test@exploresg.com
Email sent successfully
```

### Step 5: Check Auth Service Logs

```bash
docker logs -f dev-exploresg-auth-service
```

Look for:

```
Publishing UserCreatedEvent for user: Test User (userId: 999, email: test@exploresg.com)
Successfully published UserCreatedEvent for userId: 999
```

---

## 🔍 Verify Message in RabbitMQ UI

### Method 1: Check Queue Depth

1. Go to **Queues** → `exploresg.user.created`
2. Check **Messages** column (should show count)

### Method 2: Get Messages

1. Click on `exploresg.user.created` queue
2. Scroll to **Get messages** section
3. Set **Messages: 1**
4. Click **Get Message(s)**
5. View the payload:

```json
{
  "userId": 999,
  "userUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "test@exploresg.com",
  "name": "Test User",
  "givenName": "Test",
  "familyName": "User",
  "identityProvider": "GOOGLE",
  "role": "USER",
  "eventType": "USER_CREATED_TEST"
}
```

---

## 🧪 Integration Test Scenarios

### Scenario 1: Basic Connectivity

```bash
# Test 1: Status check
curl http://localhost:8080/api/v1/test/rabbitmq/status

# Expected: status = "connected"
```

### Scenario 2: Direct RabbitMQ Send

```bash
# Test 2: Direct send
curl http://localhost:8080/api/v1/test/rabbitmq/hello

# Expected: status = "success"
```

### Scenario 3: Service Layer Publish

```bash
# Test 3: Via UserEventPublisher
curl http://localhost:8080/api/v1/test/rabbitmq/publish

# Expected: status = "success"
```

### Scenario 4: Custom Message

```bash
# Test 4: Custom payload
curl -X POST http://localhost:8080/api/v1/test/rabbitmq/custom \
  -H "Content-Type: application/json" \
  -d '{"email": "custom@test.com", "name": "Custom User"}'

# Expected: status = "success"
```

### Scenario 5: Real User Creation

```bash
# Test 5: Real signup (requires valid JWT)
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Authorization: Bearer <YOUR_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+65-9123-4567",
    "dateOfBirth": "1990-01-15"
  }'

# Expected: User created + Email notification sent
```

---

## 🐛 Troubleshooting

### Error: Connection Refused

```json
{
  "status": "error",
  "message": "Failed to send test message",
  "error": "Connection refused"
}
```

**Solution:**

```bash
# Check RabbitMQ is running
docker ps | grep rabbitmq

# Check network
docker network ls | grep exploresg-net

# Restart RabbitMQ
docker restart rabbitmq
```

### Error: Authentication Failed

**Solution:**

```bash
# Check credentials in .env
echo $RABBITMQ_USERNAME
echo $RABBITMQ_PASSWORD

# Default should be guest/guest
```

### No Messages Appearing in Queue

**Possible Causes:**

1. Queue doesn't exist → Check exchange bindings
2. Wrong routing key → Verify "user.created"
3. Messages consumed immediately → Check notification service

---

## 📊 Monitoring Commands

### Check RabbitMQ Health

```bash
docker exec rabbitmq rabbitmq-diagnostics ping
```

### List Queues

```bash
docker exec rabbitmq rabbitmqctl list_queues name messages consumers
```

### List Exchanges

```bash
docker exec rabbitmq rabbitmqctl list_exchanges name type
```

### List Bindings

```bash
docker exec rabbitmq rabbitmqctl list_bindings
```

---

## 🎯 Success Criteria

✅ All tests should pass:

- [ ] `/test/rabbitmq/status` returns "connected"
- [ ] `/test/rabbitmq/hello` returns "success"
- [ ] `/test/rabbitmq/publish` returns "success"
- [ ] `/test/rabbitmq/custom` returns "success"
- [ ] Messages appear in RabbitMQ UI
- [ ] Notification service logs show email processing
- [ ] Real user creation triggers notification

---

## 🔒 Security Note

**⚠️ IMPORTANT:** The test endpoints are for development only!

### Before Production:

1. **Remove Test Controller** or secure it:

   ```java
   @PreAuthorize("hasRole('ADMIN')")
   ```

2. **Or disable in production:**

   ```java
   @Profile("!prod")
   @RestController
   public class RabbitMQTestController { ... }
   ```

3. **Or remove the file entirely**

---

## 📝 Test Report Template

```
Date: _____________
Tester: _____________

Test Results:
□ Status Check: PASS / FAIL
□ Hello Test: PASS / FAIL
□ Publish Test: PASS / FAIL
□ Custom Test: PASS / FAIL
□ Real User Test: PASS / FAIL

RabbitMQ UI Verified: YES / NO
Notification Service Verified: YES / NO
Email Received: YES / NO

Notes:
_________________________________
_________________________________
```

---

## Next Steps

After successful testing:

1. ✅ Test with real user registration
2. ✅ Verify email delivery (check inbox)
3. ✅ Monitor performance under load
4. ✅ Remove or secure test endpoints
5. ✅ Document for team

---

**Last Updated**: October 21, 2025
