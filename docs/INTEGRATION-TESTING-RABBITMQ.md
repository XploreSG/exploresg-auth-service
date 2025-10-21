# RabbitMQ Integration Testing Guide

## Overview

This guide covers testing the RabbitMQ integration for user creation events.

## Test Setup

### 1. Add Test Dependency

The `spring-boot-starter-amqp` already includes test support. For additional testing utilities, add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. Test Configuration

Create `application-test.properties`:

```properties
# Use embedded RabbitMQ for testing or Testcontainers
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

## Unit Tests

### Test UserEventPublisher

```java
@ExtendWith(MockitoExtension.class)
class UserEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserEventPublisher userEventPublisher;

    @Test
    void shouldPublishUserCreatedEvent() {
        // Given
        User user = User.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .givenName("Test")
                .familyName("User")
                .role(Role.USER)
                .identityProvider(IdentityProvider.GOOGLE)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        userEventPublisher.publishUserCreatedEvent(user);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("exploresg.user.events"),
                eq("user.created"),
                any(UserCreatedEvent.class)
        );
    }

    @Test
    void shouldHandlePublishingException() {
        // Given
        User user = createTestUser();
        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> userEventPublisher.publishUserCreatedEvent(user));
    }
}
```

### Test UserService Integration

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldPublishEventWhenNewUserCreated() {
        // Given
        Jwt jwt = createMockJwt("new@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        userService.upsertUserFromJwt(jwt, Role.USER);

        // Then
        verify(userEventPublisher, times(1)).publishUserCreatedEvent(any(User.class));
    }

    @Test
    void shouldNotPublishEventWhenUserAlreadyExists() {
        // Given
        Jwt jwt = createMockJwt("existing@example.com");
        User existingUser = createTestUser();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));

        // When
        userService.upsertUserFromJwt(jwt, Role.USER);

        // Then
        verify(userEventPublisher, never()).publishUserCreatedEvent(any(User.class));
    }
}
```

## Integration Tests

### Using Testcontainers

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
class RabbitMQIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management")
            .withExposedPorts(5672, 15672);

    @Autowired
    private UserService userService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void rabbitMQProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
    }

    @Test
    void shouldPublishMessageToRabbitMQ() throws Exception {
        // Given
        Jwt jwt = createMockJwt("integration@example.com");

        // Set up a test listener
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UserCreatedEvent> receivedEvent = new AtomicReference<>();

        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        rabbitTemplate.execute(channel -> {
            channel.basicConsume("exploresg.user.created", true,
                (consumerTag, message) -> {
                    ObjectMapper mapper = new ObjectMapper();
                    UserCreatedEvent event = mapper.readValue(message.getBody(), UserCreatedEvent.class);
                    receivedEvent.set(event);
                    latch.countDown();
                },
                consumerTag -> {});
            return null;
        });

        // When
        userService.upsertUserFromJwt(jwt, Role.USER);

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(receivedEvent.get());
        assertEquals("integration@example.com", receivedEvent.get().getEmail());
    }
}
```

### Using Embedded RabbitMQ (Alternative)

```java
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
class UserCreationEventTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldReceiveUserCreatedEvent() {
        // Setup message capture
        List<UserCreatedEvent> capturedEvents = new ArrayList<>();

        rabbitTemplate.setReceiveTimeout(5000);

        // Create user
        Jwt jwt = createMockJwt("test@example.com");
        userService.upsertUserFromJwt(jwt, Role.USER);

        // Receive and verify
        Object message = rabbitTemplate.receiveAndConvert("exploresg.user.created");
        assertNotNull(message);
        assertTrue(message instanceof UserCreatedEvent);

        UserCreatedEvent event = (UserCreatedEvent) message;
        assertEquals("test@example.com", event.getEmail());
    }
}
```

## Manual Testing

### 1. Start Services

```bash
# Start RabbitMQ (from notification service)
cd ../exploresg-notification-service
docker-compose up -d rabbitmq

# Start auth service
cd ../exploresg-auth-service
docker-compose up -d
```

### 2. Monitor RabbitMQ

Access RabbitMQ Management Console:

```
http://localhost:15672
Username: guest
Password: guest
```

### 3. Trigger User Creation

```bash
# Get a valid Google JWT token first
export JWT_TOKEN="your_google_jwt_token"

# Create user via signup
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+65-9123-4567",
    "dateOfBirth": "1990-01-15"
  }'
```

### 4. Verify Message in RabbitMQ

1. Go to **Queues** tab
2. Click on `exploresg.user.created`
3. Click **Get Messages**
4. Set "Messages: 1" and click **Get Message(s)**
5. Verify the payload

Expected payload:

```json
{
  "userId": 1,
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

## Load Testing

### Using Apache JMeter

1. Create Thread Group with 100 concurrent users
2. Add HTTP Request for user signup
3. Monitor RabbitMQ message rate
4. Verify all messages are published

### Using Artillery

```yaml
# load-test.yml
config:
  target: "http://localhost:8080"
  phases:
    - duration: 60
      arrivalRate: 10
      name: "Sustained load"

scenarios:
  - name: "Create users"
    flow:
      - post:
          url: "/api/v1/signup"
          headers:
            Authorization: "Bearer {{token}}"
            Content-Type: "application/json"
          json:
            phone: "+65-9123-4567"
            dateOfBirth: "1990-01-15"
```

Run:

```bash
artillery run load-test.yml
```

## Monitoring Tests

### Check Logs

Auth Service:

```bash
docker logs -f dev-exploresg-auth-service | grep "UserCreatedEvent"
```

Expected output:

```
Publishing UserCreatedEvent for user: John Doe (userId: 1, email: user@example.com)
Successfully published UserCreatedEvent for userId: 1
```

### Check Metrics

```bash
curl http://localhost:8080/actuator/metrics/rabbitmq.published
```

## Continuous Integration

### GitHub Actions Workflow

```yaml
- name: Start RabbitMQ
  run: |
    docker run -d --name rabbitmq \
      -p 5672:5672 -p 15672:15672 \
      rabbitmq:3-management

- name: Wait for RabbitMQ
  run: |
    timeout 30 bash -c 'until docker exec rabbitmq rabbitmq-diagnostics ping; do sleep 1; done'

- name: Run Integration Tests
  run: mvn verify -P integration-tests
  env:
    RABBITMQ_HOST: localhost
```

## Troubleshooting

### Messages Not Being Published

1. Check RabbitMQ connection:

   ```bash
   docker logs dev-exploresg-auth-service | grep "rabbit"
   ```

2. Verify exchange and queue exist:

   - RabbitMQ UI → Exchanges → `exploresg.user.events`
   - RabbitMQ UI → Queues → `exploresg.user.created`

3. Check bindings:
   - Exchange should be bound to queue with routing key `user.created`

### Test Failures

1. Ensure RabbitMQ container is running
2. Check port conflicts (5672, 15672)
3. Verify test configuration properties
4. Check firewall settings

### Slow Tests

1. Use embedded RabbitMQ instead of Testcontainers for unit tests
2. Mock RabbitTemplate for service layer tests
3. Use `@DirtiesContext` sparingly
4. Reuse containers across tests when possible

## Best Practices

1. **Isolation**: Each test should clean up its messages
2. **Timeouts**: Use appropriate timeouts for async operations
3. **Mocking**: Mock external dependencies (RabbitMQ) in unit tests
4. **Integration**: Use Testcontainers for true integration tests
5. **Coverage**: Test both happy path and error scenarios
6. **Performance**: Monitor message throughput under load
