# 📊 Logging Architecture Diagram

## Request Flow with Logging

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT REQUEST                              │
│                                                                       │
│  GET /api/v1/me                                                      │
│  Authorization: Bearer <JWT>                                         │
│  X-Correlation-ID: abc-123-def (optional)                           │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  RequestCorrelationFilter                            │
│                  (Order: HIGHEST_PRECEDENCE)                         │
│                                                                       │
│  1. Extract or generate correlation ID                               │
│  2. Add to MDC: correlationId = "abc-123-def"                       │
│  3. Add to MDC: requestMethod = "GET"                                │
│  4. Add to MDC: requestPath = "/api/v1/me"                           │
│  5. Add to MDC: clientIp = "192.168.1.100"                           │
│  6. Add to response header: X-Correlation-ID                         │
│                                                                       │
│  LOG: "Correlation ID assigned: abc-123-def"                         │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Spring Security Chain                            │
│                                                                       │
│  1. JwtAuthenticationFilter validates JWT                            │
│  2. Extracts user from token                                         │
│  3. Sets Authentication in SecurityContext                           │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  UserContextLoggingFilter                            │
│                  (Order: HIGHEST_PRECEDENCE + 10)                    │
│                                                                       │
│  1. Get Authentication from SecurityContext                          │
│  2. Extract User principal                                           │
│  3. Add to MDC: userId = "42"                                        │
│  4. Add to MDC: userEmail = "user@example.com"                       │
│                                                                       │
│  LOG: "User context added to MDC"                                    │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│              RequestLoggingInterceptor.preHandle()                   │
│                                                                       │
│  1. Start timer                                                      │
│  2. Get request details                                              │
│                                                                       │
│  LOG: "Incoming request: GET /api/v1/me from IP: 192.168.1.100"     │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        AuthController                                │
│                                                                       │
│  @GetMapping("/me")                                                  │
│  public ResponseEntity<User> getMe(@AuthenticationPrincipal User)    │
│                                                                       │
│  LOG: "Fetching user profile for userId: 42"                         │
│                                                                       │
│  - All logs automatically include MDC context:                       │
│    * correlationId                                                   │
│    * userId                                                          │
│    * userEmail                                                       │
│    * requestMethod                                                   │
│    * requestPath                                                     │
│    * clientIp                                                        │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│           RequestLoggingInterceptor.afterCompletion()                │
│                                                                       │
│  1. Calculate duration                                               │
│  2. Get response status                                              │
│  3. Check for slow requests (>2s)                                    │
│                                                                       │
│  LOG: "Completed request: GET /api/v1/me - Status: 200 - 45ms"      │
│                                                                       │
│  If slow: LOG WARN: "Slow request detected: GET /api/v1/me took 3s" │
│  If error: LOG ERROR with exception                                  │
└────────────────────────────────┬──────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Response to Client                              │
│                                                                       │
│  HTTP/1.1 200 OK                                                     │
│  X-Correlation-ID: abc-123-def                                       │
│  Content-Type: application/json                                      │
│                                                                       │
│  { "id": 42, "email": "user@example.com", ... }                      │
└─────────────────────────────────────────────────────────────────────┘

                    ALL LOGS WRITTEN TO:
                           ↓
    ┌────────────────────────────────────────────┐
    │         logback-spring.xml                 │
    │                                            │
    │  Dev Profile:                              │
    │  → Console (Human Readable + Colors)       │
    │                                            │
    │  Prod Profile:                             │
    │  → Console (JSON)                          │
    │  → File: /var/log/exploresg/auth.log       │
    │  → Error File: auth-error.log              │
    └────────────────┬───────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────┐         ┌──────────────┐
│  CloudWatch  │         │  ELK Stack   │
│    Logs      │         │  (Logstash)  │
└──────────────┘         └──────────────┘
```

---

## MDC (Mapped Diagnostic Context) Flow

```
Request Starts
     │
     ▼
┌─────────────────────────────────┐
│  MDC Context (Thread Local)     │
├─────────────────────────────────┤
│  correlationId: "abc-123-def"   │
│  requestMethod: "POST"           │
│  requestPath: "/api/v1/signup"   │
│  clientIp: "192.168.1.100"      │
├─────────────────────────────────┤
│  After Authentication:           │
│  userId: "42"                    │
│  userEmail: "user@example.com"   │
└─────────────────────────────────┘
     │
     ▼
All @Slf4j log.info/warn/error
automatically include MDC values
     │
     ▼
JSON Output:
{
  "correlationId": "abc-123-def",
  "userId": "42",
  "userEmail": "user@example.com",
  "requestMethod": "POST",
  "requestPath": "/api/v1/signup",
  "clientIp": "192.168.1.100",
  "message": "User signup completed"
}
     │
     ▼
Request Complete → MDC.clear()
```

---

## Log Levels by Environment

```
┌─────────────────────────────────────────────────────────────┐
│                  DEVELOPMENT (dev)                           │
├─────────────────────────────────────────────────────────────┤
│  Root:                    INFO                               │
│  com.exploresg:          DEBUG ← Verbose for debugging      │
│  Spring Web:             DEBUG ← See HTTP details           │
│  Spring Security:        DEBUG ← See auth flow              │
│  Hibernate:              DEBUG ← See SQL queries            │
│                                                              │
│  Format: Human Readable + Colors                            │
│  Output: Console only                                        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  STAGING                                     │
├─────────────────────────────────────────────────────────────┤
│  Root:                    INFO                               │
│  com.exploresg:          INFO ← Business logic              │
│  Spring Web:             INFO ← Important HTTP events       │
│  Spring Security:        WARN ← Only warnings/errors        │
│  Hibernate:              WARN ← Reduce noise                │
│                                                              │
│  Format: Pretty-printed JSON                                 │
│  Output: Console + Files                                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  PRODUCTION (prod)                           │
├─────────────────────────────────────────────────────────────┤
│  Root:                    INFO                               │
│  com.exploresg:          INFO ← Business events only        │
│  Spring Web:             WARN ← Reduce noise                │
│  Spring Security:        WARN ← Auth failures only          │
│  Hibernate:              WARN ← DB errors only              │
│                                                              │
│  Format: Compact JSON (one-line)                            │
│  Output: Console + Files + CloudWatch                       │
└─────────────────────────────────────────────────────────────┘
```

---

## CloudWatch Integration

```
┌──────────────────────────────────────────────────────────┐
│              Kubernetes Pod (EKS)                         │
│                                                           │
│  ┌─────────────────────────────────────────────┐        │
│  │  exploresg-auth-service container           │        │
│  │                                              │        │
│  │  STDOUT/STDERR ← JSON logs written here    │        │
│  └──────────────────┬───────────────────────────┘        │
│                     │                                     │
└─────────────────────┼─────────────────────────────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │  FluentBit DaemonSet         │
        │  (Kubernetes Log Collector)  │
        └─────────────┬────────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │  AWS CloudWatch Logs         │
# LOGGING-ARCHITECTURE.md — moved to observability

This file has been consolidated into `docs/observability.md` as part of a documentation cleanup on 2025-10-15.

The original content was archived to:

`docs/archive/2025-10-15/LOGGING-ARCHITECTURE.md`

Please see `docs/observability.md` for the combined logging, metrics, tracing, and testing guidance.

If you need the original full text, retrieve it from the archive path above.
        │  /aws/eks/exploresg/auth     │
        │                              │
        │  Log Stream: pod-name-id     │
        └─────────────┬────────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │  CloudWatch Insights         │
        │                              │
        │  Query logs by:              │
        │  - correlationId             │
        │  - userId                    │
        │  - level (ERROR/WARN/INFO)   │
        │  - requestPath               │
        │  - time range                │
        └──────────────────────────────┘
```

---

## Error Handling Flow

```
Exception Occurs in Controller/Service
            │
            ▼
┌──────────────────────────────────┐
│  @Slf4j logger in class          │
│                                  │
│  try {                           │
│    // business logic             │
│  } catch (Exception e) {         │
│    log.error("Error occurred",   │
│              e);  ← Stack trace  │
│    throw e;                      │
│  }                               │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  Global Exception Handler        │
│                                  │
│  @ControllerAdvice               │
│  Catches uncaught exceptions     │
│  Logs with full context          │
│  Returns user-friendly error     │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  RequestLoggingInterceptor       │
│  afterCompletion()               │
│                                  │
│  if (ex != null) {               │
│    log.error("Request failed",   │
│              method, path, ex);  │
│  }                               │
└────────────┬─────────────────────┘
             │
             ▼
JSON Log with Stack Trace:
{
  "timestamp": "...",
  "level": "ERROR",
  "message": "Database connection failed",
  "correlationId": "abc-123",
  "userId": "42",
  "exception": "SQLException: ...",
  "stackTrace": [
    "com.exploresg.service.UserService.getUser(UserService.java:45)",
    ...
  ]
}
             │
             ▼
Separate error log file:
/var/log/exploresg/auth-service-error.log
```

---

## Security Audit Trail

```
User Authentication Flow:

1. POST /api/v1/auth/google
   LOG: "Incoming request: POST /api/v1/auth/google from IP: x.x.x.x"

2. Google OAuth Validation
   LOG: "Validating Google OAuth token"
   LOG: "Google token validated for email: user@example.com"

3. User Lookup/Creation
   LOG: "User found/created: userId=42, email=user@example.com"

4. JWT Generation
   LOG: "JWT token generated for userId: 42"

5. Response
   LOG: "Completed request: POST /api/v1/auth/google - Status: 200 - 345ms"

All logs include:
- correlationId (trace entire flow)
- clientIp (security audit)
- timestamp (compliance)
- userId (after authentication)
```

---

**This diagram shows how every component works together to provide comprehensive logging and observability.**

See [observability.md](observability.md) for the consolidated implementation guide.
