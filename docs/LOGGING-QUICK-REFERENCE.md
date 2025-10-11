# 🚀 Logging Quick Reference

## ✅ Implementation Status: COMPLETE

Your service is now **production logging ready** with structured JSON logging, correlation IDs, and cloud integration support.

---

## 📁 Key Files

| File                             | Purpose                                  |
| -------------------------------- | ---------------------------------------- |
| `logback-spring.xml`             | Logging configuration (dev/staging/prod) |
| `RequestCorrelationFilter.java`  | Adds correlation ID to every request     |
| `UserContextLoggingFilter.java`  | Adds user info to logs                   |
| `RequestLoggingInterceptor.java` | Logs HTTP requests/responses             |
| `WebMvcConfig.java`              | Registers interceptors                   |

---

## 🎯 Quick Examples

### Dev Logs (Human Readable)

```
2025-10-11 14:23:45.123 INFO [abc-123] AuthController - User signup initiated for userId: 42
```

### Production Logs (JSON)

```json
{
  "timestamp": "2025-10-11T14:23:45.123Z",
  "level": "INFO",
  "logger": "AuthController",
  "message": "User signup initiated",
  "correlationId": "abc-123",
  "userId": "42",
  "userEmail": "user@example.com",
  "requestMethod": "POST",
  "requestPath": "/api/v1/signup"
}
```

---

## 🔍 Test It

```bash
# 1. Start service
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# 2. Make request with correlation ID
curl -X GET http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-ID: test-123"

# 3. Check logs for [test-123]
```

---

## ☁️ Cloud Ready

- ✅ AWS CloudWatch - JSON logs captured automatically
- ✅ ELK Stack - Logstash encoder included
- ✅ Datadog/New Relic - JSON format compatible
- ✅ Kubernetes - Logs to stdout/stderr

---

## 📊 What's Logged

- ✅ Request/Response with timing
- ✅ Correlation IDs for tracing
- ✅ User context (ID, email)
- ✅ Security events (auth, admin access)
- ✅ Client IP addresses
- ✅ Slow requests (>2 seconds)
- ✅ Errors with stack traces

---

## 📚 Full Documentation

See [LOGGING-GUIDE.md](LOGGING-GUIDE.md) for complete details on:

- CloudWatch Insights queries
- ELK stack integration
- Security audit logging
- Best practices
- Troubleshooting

---

**Status**: ✅ Production Ready  
**Last Updated**: October 11, 2025
