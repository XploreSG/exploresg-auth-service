# ADR-001: Using UUIDs as Public-Facing User Identifiers

**Date:** 2025-10-07
**Status:** Implemented

## 1. Summary

This document outlines the decision to introduce a `UUID`-based public identifier (`userId`) for the `User` entity. This identifier will be used in all external-facing contexts, including API endpoints and JSON Web Tokens (JWTs), to enhance security. The internal, auto-incrementing `Long id` will be retained as the primary key for optimal database performance and to minimize refactoring.

## 2. Context & Problem Statement

In our microservices architecture, there is a need for a stable user identifier that can be used by both the frontend and for service-to-service communication. The initial design used the database's primary key (`Long id`) for this purpose.

This approach presents two significant problems:

1.  **Security Vulnerability (IDOR):** Exposing an auto-incrementing, sequential ID in an API (e.g., `/api/users/123`) creates an **Insecure Direct Object Reference (IDOR)** vulnerability. Malicious actors can easily guess other valid IDs by incrementing the number (`/api/users/124`, `/api/users/125`, etc.), allowing them to enumerate users and potentially access or modify data they are not authorized to see.

2.  **Information Leakage:** A sequential ID inadvertently leaks business intelligence. For example, it can reveal the total number of users or the rate of user growth.

The goal was to find a solution that would allow us to safely expose a user identifier in JWTs and APIs without incurring these risks.

## 3. Explored Options

We considered two primary approaches to solve this problem.

### Option 1: Replace Primary Key with UUID

This option involved changing the primary key of the `app_user` table from `Long` to `UUID`.

- **Pros:**

  - Provides a single, secure identifier for a user, eliminating any confusion between a public and private ID.
  - Inherently secure against IDOR attacks.

- **Cons:**
  - **High Refactoring Cost:** Changing a primary key is a highly invasive operation that would require modifications across the entire application stack (Entities, Repositories, Services, DTOs, and Tests).
  - **Potential Performance Impact:** UUIDs are larger than `Long`s and can be less performant for database indexing and joins, especially in tables with a very high number of rows.

### Option 2: Add a Separate UUID Field for Public Use (Hybrid Approach)

This option involved keeping the existing `Long id` as the internal primary key and adding a new, separate `UUID` field to be used for all public-facing operations.

- **Pros:**

  - **Enhanced Security:** Completely mitigates the IDOR vulnerability and information leakage risks by using a non-guessable public ID.
  - **Optimal Performance:** Retains the performance benefits of using an integer as the primary key for internal database operations and foreign key relationships.
  - **Low Refactoring Cost:** This is an additive change, requiring minimal modifications. It avoids breaking changes to existing data relationships and repository methods.
  - **Clear Separation of Concerns:** Establishes a clear and simple rule: use `Long id` for internal database joins and use `UUID userId` for all external communication (APIs, DTOs, JWTs).

- **Cons:**
  - **Slightly Increased Complexity:** The `User` model now contains two identifiers. Developers must be disciplined about using the correct one in the correct context.
  - **Minor Storage Overhead:** Adds an extra column to the `app_user` table.

## 4. Decision & Rationale

**We have chosen Option 2.**

This hybrid approach was selected because it provides the full security benefits of using UUIDs externally while retaining the performance and stability of the existing integer-based primary key internally. The implementation cost is significantly lower and less risky than a full primary key replacement, making it the most pragmatic and effective solution for our needs.

## 5. Implementation Details

The implementation consisted of two main changes:

### 1. Update `User.java` Entity

A new `userId` field of type `java.util.UUID` was added. It is configured to be unique, non-nullable, and automatically initialized with a random UUID upon creation.

```java
// ... imports
import java.util.UUID;

// ...
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    @Builder.Default
    private UUID userId = UUID.randomUUID();

    // ... rest of the fields
}
```

### 2. Update `JwtService.java`

The `generateToken` method was modified to include the new `userId` as a claim in the JWT payload.

```java
// ... in JwtService.java
public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    // ... existing claims for roles

    if (userDetails instanceof User) {
        User user = (User) userDetails;
        // ... existing claims for name and picture
        extraClaims.put("userId", user.getUserId()); // <-- Added userId to JWT
    }

    return buildToken(extraClaims, userDetails, jwtExpiration);
}
```
