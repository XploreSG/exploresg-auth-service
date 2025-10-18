# EXPLORE-11 — Signup Profile & Google SSO Onboarding

Status: Draft
Assignee: @TODO
Sprint: EXPLORE

## Goal

Implement end-to-end signup/profile onboarding for Google SSO users and email/password users, expose the signup API, persist profile data, and add tests and docs.

## Scope

- API to create/update user profile after SSO login or email signup.
- Persist profile in `user_profile` (one-to-one with `app_user`).
- Validate input and return `SignupResponse`.
- Keep Google SSO flow that upserts users from JWT.

## Acceptance Criteria

1. POST /api/v1/signup accepts a validated body and returns 200 with `SignupResponse`.
2. If user exists, profile is updated; otherwise created.
3. Name overrides from signup form update `app_user` if provided.
4. Validation errors return HTTP 400 with field errors (handled by `GlobalExceptionHandler`).
5. Integration tests exercise happy path and validation failures.

## API Contract

- Endpoint: POST /api/v1/signup
- Auth: OAuth2 JWT (same as current `AuthController` flow)
- Request body: `SignupProfileRequest` — see [`com.exploresg.authservice.dto.SignupProfileRequest`](src/main/java/com/exploresg/authservice/dto/SignupProfileRequest.java)
- Response body: `SignupResponse` — see [`com.exploresg.authservice.dto.SignupResponse`](src/main/java/com/exploresg/authservice/dto/SignupResponse.java)
- Server code: [`com.exploresg.authservice.controller.AuthController`](src/main/java/com/exploresg/authservice/controller/AuthController.java) and [`com.exploresg.authservice.service.UserService`](src/main/java/com/exploresg/authservice/service/UserService.java)

Example curl (dev) — requires valid JWT:
curl -X POST "http://localhost:8080/api/v1/signup" -H "Authorization: Bearer <jwt>" -H "Content-Type: application/json" -d '{"phone":"1234","dateOfBirth":"1990-01-01","drivingLicenseNumber":"DL12345"}'

## Database / Schema

- `app_user` table already defined by [`com.exploresg.authservice.model.User`](src/main/java/com/exploresg/authservice/model/User.java).
- `user_profile` is defined by [`com.exploresg.authservice.model.UserProfile`](src/main/java/com/exploresg/authservice/model/UserProfile.java) and maps id -> user.id via `@MapsId`.
- Add migration (Flyway or Liquibase) in CI to create `user_profile` if using Postgres in production.

## Files to review / update

- Controller: [`com.exploresg.authservice.controller.AuthController`](src/main/java/com/exploresg/authservice/controller/AuthController.java)
- Service: [`com.exploresg.authservice.service.UserService`](src/main/java/com/exploresg/authservice/service/UserService.java)
- DTOs: [`com.exploresg.authservice.dto.SignupProfileRequest`](src/main/java/com/exploresg/authservice/dto/SignupProfileRequest.java), [`com.exploresg.authservice.dto.SignupResponse`](src/main/java/com/exploresg/authservice/dto/SignupResponse.java)
- Models: [`com.exploresg.authservice.model.UserProfile`](src/main/java/com/exploresg/authservice/model/UserProfile.java), [`com.exploresg.authservice.model.User`](src/main/java/com/exploresg/authservice/model/User.java)
- Repositories: [`com.exploresg.authservice.repository.UserProfileRepository`](src/main/java/com/exploresg/authservice/repository/UserProfileRepository.java), [`com.exploresg.authservice.repository.UserRepository`](src/main/java/com/exploresg/authservice/repository/UserRepository.java)
- Exception handling: [`com.exploresg.authservice.exception.GlobalExceptionHandler`](src/main/java/com/exploresg/authservice/exception/GlobalExceptionHandler.java)

## Tests

- Unit tests for `UserService.createOrUpdateProfile` (happy path, name override, invalid user).
- Integration tests (Spring Boot Test with H2) for the `/api/v1/signup` endpoint:
  - Happy path: JWT present → user upsert → profile created.
  - Validation errors: missing phone/dateOfBirth/drivingLicenseNumber → 400 with field map.
- Example tests should live under `src/test/java/com/exploresg/authservice/`.

## Dev & Run

- Local DB: use `docker-compose.yml` — [docker-compose.yml](docker-compose.yml)
- Build: `mvn clean package`
- Run: `docker-compose up --build` or `mvn spring-boot:run`
- Check endpoints: `/hello`, `/ping`, `/health` — see [`com.exploresg.authservice.controller.HelloWorldController`](src/main/java/com/exploresg/authservice/controller/HelloWorldController.java)

## PR Template (for EXPLORE-11)

- Summary of changes
- Files changed (controllers, service, dto, repo, migration)
- How to run locally (docker-compose + sample curl)
- Tests added and pass locally
- Checklist: docs updated, migrations included, CI green

## Notes

- Security: ensure `SecurityConfig` allows intended public endpoints — see [`com.exploresg.authservice.config.SecurityConfig`](src/main/java/com/exploresg/authservice/config/SecurityConfig.java)
- CORS: dev origin configured in [`com.exploresg.authservice.config.CorsConfig`](src/main/java/com/exploresg/authservice/config/CorsConfig.java)
- Update [UC-001.md](UC-001.md) and [README.md](README.md) to reference the new endpoint and flow.
