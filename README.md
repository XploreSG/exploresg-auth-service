# ExploreSG Authentication Service

[![CI Backend - Build, Test & Security Scan](https://github.com/XploreSG/exploresg-auth-service/actions/workflows/ci-java.yml/badge.svg)](https://github.com/XploreSG/exploresg-auth-service/actions/workflows/ci-java.yml)

A production-ready, secure authentication and authorization microservice built for the Explore Singapore platform. This service handles user onboarding via Google SSO, issues custom JSON Web Tokens (JWTs) for session management, and provides fine-grained, role-based access control (RBAC).

## 📋 Table of Contents

- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Architecture Overview](#-architecture-overview)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Configuration](#-configuration)
- [Testing](#-testing)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Security](#-security)
- [Development Guidelines](#-development-guidelines)
- [Contributing](#-contributing)

## ✨ Features

| Feature                       | Description                                                               |
| ----------------------------- | ------------------------------------------------------------------------- |
| **Google SSO Integration**    | Seamless authentication using Google OAuth 2.0                            |
| **Custom JWT Generation**     | Creates secure, stateless JWTs with user details, roles, and metadata     |
| **Role-Based Access Control** | Supports multiple roles (USER, ADMIN, FLEET_MANAGER, SUPPORT, MANAGER)    |
| **User Profile Management**   | Complete profile system with personal details, documents, and preferences |
| **Token Exchange Flow**       | Trades Google ID tokens for custom application JWTs                       |
| **Database Integration**      | PostgreSQL with Spring Data JPA and Hibernate                             |
| **Comprehensive Testing**     | Unit, integration, and security tests with JaCoCo coverage                |
| **CI/CD Ready**               | GitHub Actions workflows for automated testing and deployment             |
| **Docker Support**            | Fully containerized with Docker Compose for easy deployment               |
| **API Documentation**         | OpenAPI/Swagger documentation available                                   |

## 🛠️ Technology Stack

| Category                 | Technologies                                                 |
| ------------------------ | ------------------------------------------------------------ |
| **Language & Framework** | Java 17, Spring Boot 3.5.6                                   |
| **Security**             | Spring Security 6, OAuth2 Resource Server, JWT (jjwt 0.11.5) |
| **Database**             | PostgreSQL 15, H2 (testing), Spring Data JPA, Hibernate      |
| **Build & Dependencies** | Maven 3.9.11, Lombok                                         |
| **Testing**              | JUnit 5, Spring Boot Test, JaCoCo, Failsafe                  |
| **Documentation**        | SpringDoc OpenAPI 2.2.0                                      |
| **DevOps**               | Docker, Docker Compose, GitHub Actions                       |

## 🏗️ Architecture Overview

```
┌─────────────────┐
│   Frontend      │
│   (React/Web)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   API Gateway   │
│   (Future)      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  Auth Service (This Repo)   │
│  ┌─────────────────────┐   │
│  │  Controllers        │   │
│  │  - Auth             │   │
│  │  - User Profile     │   │
│  └─────────────────────┘   │
│  ┌─────────────────────┐   │
│  │  Services           │   │
│  │  - Authentication   │   │
│  │  - JWT              │   │
│  │  - User             │   │
│  └─────────────────────┘   │
│  ┌─────────────────────┐   │
│  │  Security           │   │
│  │  - JWT Filter       │   │
│  │  - OAuth2 Config    │   │
│  └─────────────────────┘   │
└──────────┬──────────────────┘
           │
           ▼
    ┌──────────────┐
    │  PostgreSQL  │
    │   Database   │
    └──────────────┘
```

## 🚀 Getting Started

### Prerequisites

| Requirement         | Version | Purpose                           |
| ------------------- | ------- | --------------------------------- |
| Java JDK            | 17+     | Runtime environment               |
| Docker              | Latest  | Containerization                  |
| Docker Compose      | Latest  | Multi-container orchestration     |
| Maven               | 3.9+    | Build tool (included via wrapper) |
| Google OAuth Client | -       | Authentication provider           |

### Quick Start (Docker Compose)

1. **Clone the Repository**

   ```bash
   git clone https://github.com/XploreSG/exploresg-auth-service.git
   cd exploresg-auth-service
   ```

2. **Configure Environment Variables**

   Create a `.env` file or set the following environment variables:

   ```bash
   SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/exploresg-auth-service-db
   SPRING_DATASOURCE_USERNAME=exploresguser
   SPRING_DATASOURCE_PASSWORD=exploresgpass
   ```

3. **Start the Application**

   ```bash
   docker-compose up --build
   ```

4. **Verify the Service**
   ```bash
   curl http://localhost:8080/health
   ```

### Local Development Setup

1. **Build the Project**

   ```bash
   ./mvnw clean package
   ```

2. **Run Tests**

   ```bash
   ./mvnw test
   ```

3. **Run the Application**

   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

## 📡 API Documentation

### Base URL

```
http://localhost:8080/api/v1
```

### Authentication Endpoints

| Method | Endpoint       | Auth Required | Description                             |
| ------ | -------------- | ------------- | --------------------------------------- |
| `POST` | `/auth/google` | No            | Exchange Google ID token for custom JWT |

**Request Example:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Authorization: Bearer <GOOGLE_ID_TOKEN>"
```

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "requiresProfileSetup": true,
  "userInfo": {
    "userId": 1,
    "email": "user@example.com",
    "givenName": "John",
    "familyName": "Doe",
    "picture": "https://...",
    "phone": null,
    "dateOfBirth": null,
    "drivingLicenseNumber": null,
    "passportNumber": null,
    "preferredLanguage": null,
    "countryOfResidence": null
  }
}
```

### User Management Endpoints

| Method | Endpoint               | Auth Required | Roles | Description                |
| ------ | ---------------------- | ------------- | ----- | -------------------------- |
| `POST` | `/signup`              | Yes (JWT)     | Any   | Create/update user profile |
| `GET`  | `/me`                  | Yes (JWT)     | Any   | Get current user details   |
| `GET`  | `/check?email=<email>` | No            | -     | Check if email exists      |

**Signup Example:**

```bash
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "givenName": "John",
    "familyName": "Doe",
    "phone": "+65-9123-4567",
    "dateOfBirth": "1990-01-15",
    "drivingLicenseNumber": "S1234567A",
    "passportNumber": "E1234567",
    "preferredLanguage": "en",
    "countryOfResidence": "Singapore"
  }'
```

### Role-Protected Endpoints

| Method | Endpoint           | Roles Required       | Description             |
| ------ | ------------------ | -------------------- | ----------------------- |
| `GET`  | `/admin/dashboard` | ADMIN                | Admin dashboard access  |
| `GET`  | `/fleet/vehicles`  | FLEET_MANAGER, ADMIN | Fleet management access |

### Health & Monitoring Endpoints

| Method | Endpoint           | Auth Required | Description                 |
| ------ | ------------------ | ------------- | --------------------------- |
| `GET`  | `/hello`           | No            | Simple health check         |
| `GET`  | `/ping`            | No            | Ping-pong endpoint          |
| `GET`  | `/health`          | No            | Detailed health status      |
| `GET`  | `/actuator/health` | No            | Spring Boot actuator health |

## 🗄️ Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐
│    app_user     │
├─────────────────┤
│ id (PK)         │
│ email (UK)      │
│ role            │
│ password        │
│ name            │
│ given_name      │
│ family_name     │
│ picture         │
│ google_sub (UK) │
│ is_active       │
│ identity_provider│
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │ 1:1
         ▼
┌─────────────────┐
│  user_profile   │
├─────────────────┤
│ id (PK, FK)     │
│ phone           │
│ date_of_birth   │
│ driving_license │
│ passport_number │
│ preferred_lang  │
│ country         │
│ created_at      │
│ updated_at      │
└─────────────────┘

         ┌────────┐
         │ 1:N    │
         ▼        │
┌─────────────────┐│
│   auth_token    ││
├─────────────────┤│
│ id (PK)         ││
│ user_id (FK)    ││
│ token_value (UK)││
│ is_revoked      ││
│ expires_at      ││
│ created_at      │┘
└─────────────────┘
```

### Table Definitions

#### `app_user`

| Column              | Type         | Constraints            | Description                   |
| ------------------- | ------------ | ---------------------- | ----------------------------- |
| `id`                | BIGSERIAL    | PRIMARY KEY            | Auto-incrementing user ID     |
| `email`             | VARCHAR(255) | UNIQUE, NOT NULL       | User's email address          |
| `role`              | VARCHAR(255) | NOT NULL               | User role (USER, ADMIN, etc.) |
| `password`          | VARCHAR(255) | -                      | Hashed password (future use)  |
| `name`              | VARCHAR(255) | -                      | Full name                     |
| `given_name`        | VARCHAR(255) | -                      | First name                    |
| `family_name`       | VARCHAR(255) | -                      | Last name                     |
| `picture`           | VARCHAR(255) | -                      | Profile picture URL           |
| `google_sub`        | VARCHAR(255) | UNIQUE                 | Google subject ID             |
| `is_active`         | BOOLEAN      | NOT NULL, DEFAULT true | Account status                |
| `identity_provider` | VARCHAR(255) | NOT NULL               | GOOGLE, LOCAL, GITHUB         |
| `created_at`        | TIMESTAMP    | NOT NULL               | Account creation time         |
| `updated_at`        | TIMESTAMP    | NOT NULL               | Last update time              |

#### `user_profile`

| Column                   | Type         | Constraints              | Description            |
| ------------------------ | ------------ | ------------------------ | ---------------------- |
| `id`                     | BIGINT       | PRIMARY KEY, FOREIGN KEY | References app_user.id |
| `phone`                  | VARCHAR(255) | -                        | Contact number         |
| `date_of_birth`          | DATE         | -                        | User's birthdate       |
| `driving_license_number` | VARCHAR(255) | -                        | Driver's license       |
| `passport_number`        | VARCHAR(255) | -                        | Passport (optional)    |
| `preferred_language`     | VARCHAR(255) | -                        | Language preference    |
| `country_of_residence`   | VARCHAR(255) | -                        | Country of residence   |
| `created_at`             | TIMESTAMP    | NOT NULL                 | Profile creation time  |
| `updated_at`             | TIMESTAMP    | NOT NULL                 | Last update time       |

#### `auth_token`

| Column        | Type      | Constraints             | Description            |
| ------------- | --------- | ----------------------- | ---------------------- |
| `id`          | BIGSERIAL | PRIMARY KEY             | Token ID               |
| `user_id`     | BIGINT    | FOREIGN KEY, NOT NULL   | References app_user.id |
| `token_value` | VARCHAR   | UNIQUE, NOT NULL        | JWT token string       |
| `is_revoked`  | BOOLEAN   | NOT NULL, DEFAULT false | Revocation status      |
| `expires_at`  | TIMESTAMP | NOT NULL                | Token expiration       |
| `created_at`  | TIMESTAMP | NOT NULL                | Token creation time    |

## ⚙️ Configuration

### Application Properties

| Property                                               | Environment Variable         | Default                       | Description                                   |
| ------------------------------------------------------ | ---------------------------- | ----------------------------- | --------------------------------------------- |
| `spring.datasource.url`                                | `SPRING_DATASOURCE_URL`      | -                             | PostgreSQL connection URL                     |
| `spring.datasource.username`                           | `SPRING_DATASOURCE_USERNAME` | -                             | Database username                             |
| `spring.datasource.password`                           | `SPRING_DATASOURCE_PASSWORD` | -                             | Database password                             |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | -                            | `https://accounts.google.com` | Google OAuth issuer                           |
| `spring.security.oauth2.resourceserver.jwt.audiences`  | -                            | Your Client ID                | Google OAuth client ID                        |
| `application.security.jwt.secret-key`                  | -                            | -                             | JWT signing secret (**change in production**) |
| `application.security.jwt.expiration`                  | -                            | `86400000`                    | JWT expiration (24 hours)                     |
| `application.security.jwt.refresh-token.expiration`    | -                            | `604800000`                   | Refresh token expiration (7 days)             |

### Environment-Specific Profiles

| Profile             | Purpose                     | Activation                                  |
| ------------------- | --------------------------- | ------------------------------------------- |
| `default`           | Production configuration    | Default                                     |
| `integration-test`  | Integration testing with H2 | `-Dspring.profiles.active=integration-test` |
| `local-integration` | Local development testing   | `-P local-integration`                      |

### Docker Compose Configuration

```yaml
services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: exploresg-auth-service-db
      POSTGRES_USER: exploresguser
      POSTGRES_PASSWORD: exploresgpass
    ports:
      - "5432:5432"
    volumes:
      - xpl-auth-pgdata:/var/lib/postgresql/data

  backend-auth-dev:
    build: .
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/exploresg-auth-service-db
      SPRING_DATASOURCE_USERNAME: exploresguser
      SPRING_DATASOURCE_PASSWORD: exploresgpass
    ports:
      - "8080:8080"
    networks:
      - default
      - exploresg-network
```

## 🧪 Testing

### Test Categories

| Category              | Framework                          | Coverage                 | Purpose                               |
| --------------------- | ---------------------------------- | ------------------------ | ------------------------------------- |
| **Unit Tests**        | JUnit 5, Mockito                   | Model, Service, Security | Test individual components            |
| **Integration Tests** | Spring Boot Test, TestRestTemplate | API, Database            | Test complete workflows               |
| **Security Tests**    | Spring Security Test               | JWT, OAuth2              | Validate authentication/authorization |

### Running Tests

#### All Tests

```bash
./mvnw test
```

#### Unit Tests Only

```bash
./mvnw test -Dtest='!*Integration*'
```

#### Integration Tests Only

```bash
./mvnw test -Dtest='*Integration*Test' -Dspring.profiles.active=integration-test
```

#### With Coverage Report

```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index.html
```

#### Using Test Scripts (Cross-Platform)

**Linux/Mac:**

```bash
chmod +x run-integration-tests.sh
./run-integration-tests.sh hello-world
./run-integration-tests.sh all --debug
./run-integration-tests.sh coverage
```

**Windows:**

```cmd
run-integration-tests.bat hello-world
run-integration-tests.bat all --debug
run-integration-tests.bat coverage
```

### Test Commands Reference

| Command                                  | Description                    |
| ---------------------------------------- | ------------------------------ |
| `./run-integration-tests.sh hello-world` | Run basic integration tests    |
| `./run-integration-tests.sh database`    | Run database integration tests |
| `./run-integration-tests.sh all`         | Run all integration tests      |
| `./run-integration-tests.sh unit`        | Run unit tests only            |
| `./run-integration-tests.sh coverage`    | Generate coverage report       |
| `./run-integration-tests.sh clean`       | Clean and rebuild              |

### GitHub Actions Integration Tests

Manually trigger comprehensive integration tests via GitHub Actions:

1. Navigate to **Actions** tab in GitHub
2. Select **"Integration Tests - Auth Service"**
3. Click **"Run workflow"**
4. Configure:
   - **Test Environment:** integration/staging/local-simulation
   - **Test Suite:** all/hello-world/database-integration/api-endpoints
   - **Log Level:** DEBUG/INFO/WARN/ERROR
5. Download test reports and coverage from artifacts

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows

| Workflow                    | Trigger                     | Purpose                           |
| --------------------------- | --------------------------- | --------------------------------- |
| **CI - Build, Test & Scan** | Push to main/feature/hotfix | Build, test, security scan        |
| **Docker Build & Publish**  | Push to main/release        | Build and push Docker images      |
| **Integration Tests**       | Manual trigger              | Comprehensive integration testing |

### CI Pipeline Stages

```
┌──────────────┐
│   Setup      │ → Install Java 17, Cache Maven dependencies
└──────┬───────┘
       │
┌──────▼───────┐
│   Lint       │ → Run static analysis (checkstyle, spotbugs, pmd)
└──────┬───────┘
       │
┌──────▼───────┐
│  Unit Tests  │ → Run JUnit tests, Generate JaCoCo report
└──────┬───────┘
       │
┌──────▼───────┐
│    Build     │ → Maven package, Build Docker image
└──────┬───────┘
       │
┌──────▼───────┐
│   Security   │ → OWASP dependency check, CodeQL SAST (optional)
└──────────────┘
```

### Build Artifacts

| Artifact         | Retention | Location                   |
| ---------------- | --------- | -------------------------- |
| JAR files        | 1 day     | `target/*.jar`             |
| Coverage reports | 7 days    | `target/site/jacoco/`      |
| Test reports     | 30 days   | `target/surefire-reports/` |
| Docker images    | -         | Docker Hub                 |

## 🔒 Security

### Authentication Flow

```
1. Frontend → Send Google ID Token
                ↓
2. Auth Service → Validate with Google OAuth
                ↓
3. Auth Service → Upsert User in Database
                ↓
4. Auth Service → Generate Custom JWT
                ↓
5. Auth Service → Return JWT + User Info
                ↓
6. Frontend → Store JWT (localStorage/cookie)
                ↓
7. Frontend → Send JWT with each request
                ↓
8. Auth Service → Validate JWT & Extract Claims
                ↓
9. Auth Service → Authorize based on Roles
```

### Security Features

| Feature                | Implementation                                   |
| ---------------------- | ------------------------------------------------ |
| **Password Hashing**   | BCrypt (future local auth)                       |
| **JWT Signing**        | HS256 with secret key                            |
| **Token Validation**   | Signature, expiration, issuer checks             |
| **Role-Based Access**  | `@PreAuthorize` annotations                      |
| **CORS Configuration** | Configured for localhost:3000                    |
| **CSRF Protection**    | Disabled for stateless JWT auth                  |
| **HTTPS**              | Required in production (configure reverse proxy) |

### JWT Claims Structure

```json
{
  "sub": "user@example.com",
  "roles": ["ROLE_USER"],
  "givenName": "John",
  "familyName": "Doe",
  "picture": "https://...",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Roles & Permissions

| Role            | Description   | Typical Use Case             |
| --------------- | ------------- | ---------------------------- |
| `USER`          | Standard user | Regular platform access      |
| `ADMIN`         | Administrator | Full system access           |
| `FLEET_MANAGER` | Fleet manager | Manage vehicles and bookings |
| `SUPPORT`       | Support staff | Customer support access      |
| `MANAGER`       | Manager       | Business operations          |

## 👨‍💻 Development Guidelines

### Project Structure

```
exploresg-auth-service/
├── .github/workflows/       # CI/CD pipelines
├── docs/                    # Additional documentation
├── src/
│   ├── main/
│   │   ├── java/com/exploresg/authservice/
│   │   │   ├── config/     # Security, JWT, CORS config
│   │   │   ├── controller/ # REST controllers
│   │   │   ├── dto/        # Data transfer objects
│   │   │   ├── entity/     # JPA entities (legacy)
│   │   │   ├── model/      # Domain models
│   │   │   ├── repository/ # JPA repositories
│   │   │   ├── service/    # Business logic
│   │   │   └── exception/  # Global exception handling
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/           # Unit & integration tests
│       └── resources/      # Test configurations
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

### Code Style & Standards

| Aspect             | Standard                                     |
| ------------------ | -------------------------------------------- |
| **Java Version**   | 17 (LTS)                                     |
| **Code Style**     | Spring Boot conventions                      |
| **Naming**         | CamelCase for classes, camelCase for methods |
| **Annotations**    | Lombok for boilerplate reduction             |
| **REST API**       | RESTful principles, HTTP status codes        |
| **Error Handling** | Global exception handler                     |
| **Documentation**  | Javadoc for public APIs                      |

### Adding New Features

1. **Create Feature Branch**

   ```bash
   git checkout -b feature/EXPLORE-XXX-description
   ```

2. **Implement Changes**

   - Add models/entities
   - Create/update repositories
   - Implement service logic
   - Add controller endpoints
   - Write unit tests
   - Write integration tests

3. **Test Locally**

   ```bash
   ./mvnw test
   ./mvnw spring-boot:run
   ```

4. **Update Documentation**

   - Update README.md
   - Add API documentation
   - Update Swagger annotations

5. **Create Pull Request**
   - Ensure CI passes
   - Request code review
   - Address feedback

### Database Migrations

For production deployments, use Flyway or Liquibase:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Place migration scripts in `src/main/resources/db/migration/`:

```
V1__Initial_schema.sql
V2__Add_user_profile.sql
V3__Add_auth_tokens.sql
```

## 🤝 Contributing

### Contribution Workflow

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Ensure all tests pass
6. Update documentation
7. Submit a pull request

### Pull Request Checklist

- [ ] Code follows project conventions
- [ ] All tests pass locally
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] No security vulnerabilities introduced
- [ ] CI pipeline passes
- [ ] Code reviewed by at least one team member

### Reporting Issues

When reporting issues, include:

- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Java version, etc.)
- Relevant logs or screenshots

## 📚 Additional Resources

| Resource                   | Link                                                                               |
| -------------------------- | ---------------------------------------------------------------------------------- |
| **API Documentation**      | `/swagger-ui/index.html` (when running)                                            |
| **Integration Test Guide** | [INTEGRATION-TESTS-README.md](INTEGRATION-TESTS-README.md)                         |
| **Integration Test Setup** | [INTEGRATION-TEST-SETUP-SUMMARY.md](INTEGRATION-TEST-SETUP-SUMMARY.md)             |
| **Use Case Documentation** | [UC-001.md](UC-001.md)                                                             |
| **Signup Feature Spec**    | [docs/EXPLORE-11.md](docs/EXPLORE-11%20—%20Signup%20Profile%20&%20Google%20SSO.md) |
| **Spring Boot Docs**       | https://spring.io/projects/spring-boot                                             |
| **Spring Security**        | https://spring.io/projects/spring-security                                         |

## 📝 License

This project is proprietary software developed for the ExploreSG platform.

## 🆘 Support

For issues, questions, or contributions:

- **Issues:** Use GitHub Issues
- **Documentation:** Check `/docs` directory
- **Email:** [support@exploresg.com](mailto:support@exploresg.com)

---

**Built with ❤️ for ExploreSG Platform**
