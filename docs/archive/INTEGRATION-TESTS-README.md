# 🧪 Integration Tests - ExploreSG Auth Service

## Overview

This directory contains industry-grade integration tests for the ExploreSG Authentication Service. The integration tests are designed to validate the complete functionality of the service including database operations, REST API endpoints, and service interactions.

## 🚀 Quick Start

### Running Tests Locally

```bash
# Run all integration tests
./mvnw test -Dtest="*Integration*Test" -Dspring.profiles.active=integration-test

# Run specific test suites
./mvnw test -Dtest="HelloWorldIntegrationTest"
./mvnw test -Dtest="AuthServiceDatabaseIntegrationTest"

# Run with Maven profiles
./mvnw test -P integration-tests
./mvnw test -P local-integration
```

### Running via GitHub Actions

1. Go to the **Actions** tab in your GitHub repository
2. Select **🚀 Integration Tests - Auth Service**
3. Click **Run workflow**
4. Choose your test configuration:
   - **Test Environment**: integration/staging/local-simulation
   - **Test Suite**: all/hello-world/database-integration/api-endpoints
   - **Log Level**: DEBUG/INFO/WARN/ERROR
5. Click **Run workflow**

## 📁 Test Structure

```
src/test/java/com/exploresg/authservice/integration/
├── HelloWorldIntegrationTest.java          # Basic endpoint and health checks
├── AuthServiceDatabaseIntegrationTest.java # Database operations and CRUD tests
├── config/
│   └── IntegrationTestConfig.java          # Test-specific Spring configuration
└── resources/
    └── application-integration-test.properties # Test profile configuration
```

## 🧪 Test Categories

### 1. Hello World Integration Tests (`HelloWorldIntegrationTest`)

**Purpose**: Basic smoke tests to ensure the application starts and core endpoints work

**Tests Include**:

- Application context loading
- Basic HTTP endpoint responses (`/hello`, `/ping`, `/health`)
- Public API endpoint functionality (`/api/v1/check`)
- Security endpoint behavior (unauthorized access)
- Actuator health endpoint validation

**Usage**:

```bash
./mvnw test -Dtest="HelloWorldIntegrationTest"
```

### 2. Database Integration Tests (`AuthServiceDatabaseIntegrationTest`)

**Purpose**: Comprehensive database operations and data persistence validation

**Tests Include**:

- Database connectivity and clean state verification
- User entity CRUD operations
- Database constraint enforcement
- Repository method validation
- Multi-user scenarios
- Database-dependent endpoint testing

**Usage**:

```bash
./mvnw test -Dtest="AuthServiceDatabaseIntegrationTest"
```

## ⚙️ Configuration

### Test Profiles

#### `integration-test` Profile

- Uses H2 in-memory database for fast, isolated testing
- Test-specific security configuration
- Enhanced logging for debugging
- Optimized for CI/CD environments

#### Key Configuration (`application-integration-test.properties`)

```properties
# H2 Database for testing
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Test JWT Configuration
application.security.jwt.secret-key=testSecretKey...
application.security.jwt.expiration=3600000
```

### Maven Profiles

#### Integration Tests Profile (`-P integration-tests`)

```bash
./mvnw test -P integration-tests
```

#### CI Profile (`-P ci`)

- Optimized for GitHub Actions
- Enhanced JaCoCo coverage reporting
- Merged test reports

```bash
./mvnw test -P ci
```

#### Local Development Profile (`-P local-integration`)

```bash
./mvnw test -P local-integration
```

## 📊 Test Reporting

### Coverage Reports

- **JaCoCo**: Comprehensive code coverage analysis
- **Surefire**: Detailed test execution reports
- **Failsafe**: Integration test specific reporting

### Generated Reports Location

```
target/
├── surefire-reports/          # Unit test reports
├── failsafe-reports/          # Integration test reports
└── site/jacoco/              # Coverage reports
    ├── index.html            # Main coverage report
    ├── jacoco.xml           # XML format for CI
    └── jacoco.csv           # CSV format for analysis
```

### Viewing Reports Locally

```bash
# Generate and view coverage report
./mvnw test jacoco:report
open target/site/jacoco/index.html

# View test results
open target/surefire-reports/index.html
```

## 🔧 Advanced Usage

### Custom Test Properties

```bash
# Run with custom database
./mvnw test -Dspring.datasource.url=jdbc:postgresql://localhost:5432/test_db

# Run with debug logging
./mvnw test -Dlogging.level.com.exploresg=DEBUG

# Run with specific profile
./mvnw test -Dspring.profiles.active=integration-test,debug
```

### Docker Integration

```bash
# Run with PostgreSQL container
docker run -d --name test-postgres -p 5432:5432 -e POSTGRES_DB=test_auth_db -e POSTGRES_USER=testuser -e POSTGRES_PASSWORD=testpass postgres:15

# Run tests against real database
./mvnw test -Dspring.datasource.url=jdbc:postgresql://localhost:5432/test_auth_db
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Tests Fail Due to Port Conflicts

```bash
# Check for running services on port 8080
lsof -i :8080
# Kill conflicting processes or use random port
./mvnw test -Dserver.port=0
```

#### 2. Database Connection Issues

```bash
# Verify H2 dependency is available
./mvnw dependency:tree | grep h2
# Check test profile activation
./mvnw test -X | grep "integration-test"
```

#### 3. Security Configuration Issues

- Ensure `IntegrationTestConfig.java` is properly loaded
- Verify `@ActiveProfiles("integration-test")` annotation
- Check security logs with debug level

### Debug Mode

```bash
# Run tests in debug mode
./mvnw test -Dtest="HelloWorldIntegrationTest" -X -Dlogging.level.root=DEBUG
```

## 🎯 Best Practices

### 1. Test Isolation

- Each test method is transactional and rolls back automatically
- Database is clean before each test
- No shared state between test methods

### 2. Test Data Management

- Use builders for creating test entities
- Prefer in-memory database for speed
- Clean up resources in `@BeforeEach` methods

### 3. Assertion Strategy

- Use AssertJ for fluent assertions
- Test both happy path and error scenarios
- Verify HTTP status codes and response bodies

### 4. Performance Considerations

- Tests run with `@Transactional` for automatic rollback
- H2 database provides fast test execution
- Parallel execution supported (configure in `pom.xml`)

## 📈 Continuous Improvement

### Expanding Test Coverage

1. **Add New Integration Tests**:

   ```java
   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   @ActiveProfiles("integration-test")
   public class NewFeatureIntegrationTest {
       // Your tests here
   }
   ```

2. **Add Performance Tests**:

   - Use `@Timed` annotations
   - Add load testing scenarios
   - Monitor response times

3. **Add Security Tests**:
   - JWT token validation
   - OAuth2 integration
   - Role-based access control

### Monitoring and Metrics

- Monitor test execution times
- Track test coverage trends
- Analyze failure patterns

## 🤝 Contributing

When adding new integration tests:

1. Follow the existing naming convention (`*IntegrationTest.java`)
2. Use the `integration-test` profile
3. Include comprehensive assertions
4. Document test purpose and scenarios
5. Update this README with new test categories

---

## 📞 Support

For issues with integration tests:

1. Check the troubleshooting section above
2. Review test logs in GitHub Actions artifacts
3. Ensure all dependencies are properly configured
4. Verify environment-specific configurations

**Happy Testing! 🚀**
