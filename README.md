[![CI Backend - Build, Test & Security Scan](https://github.com/XploreSG/exploresg-auth-service/actions/workflows/ci-java.yml/badge.svg)](https://github.com/XploreSG/exploresg-auth-service/actions/workflows/ci-java.yml)

# ExploreSG Auth Service

A robust, secure authentication and authorization microservice built for the Explore Singapore platform. This service handles user onboarding via Google SSO, issues custom JSON Web Tokens (JWTs) for session management, and provides fine-grained, role-based access control (RBAC).

## ✨ Features

- **Google SSO Integration**: Authenticates users securely using their Google accounts.
- **Token Exchange Flow**: Trades a short-lived Google ID token for a secure, custom application JWT.
- **Custom JWT Generation**: Creates custom, stateless JWTs containing user details like name, picture, and roles.
- **Multi-Role-Based Access Control (RBAC)**: Supports multiple roles per user (e.g., `USER`, `ADMIN`, `FLEET_MANAGER`) with endpoint protection using `@PreAuthorize` annotations.
- **User & Profile Management**: Handles user creation and manages a separate, detailed user profile with information like phone number and driver's license.
- **Database Integration**: Uses Spring Data JPA and Hibernate to persist user data in a PostgreSQL database.
- **Dockerized**: Comes with `docker-compose.yml` for easy, one-command setup of the service and its database.

## 🛠️ Tech Stack

- **Java 17** & **Spring Boot 3**
- **Spring Security 6** (OAuth2 Resource Server & Custom JWT Filters)
- **Spring Data JPA / Hibernate**
- **PostgreSQL**
- **JSON Web Tokens (jjwt)**
- **Docker & Docker Compose**

<br>


## 🚀 Getting Started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) are installed.
- You have a **Google OAuth 2.0 Client ID**.

### Setup & Installation

1.  **Clone the Repository**

    ```bash
    git clone <your-repo-url>
    cd exploresg-auth-service
    ```

2.  **Run with Docker Compose**
    From the root directory, run the following command:

    ```bash
    docker-compose up --build
    ```

    The service will start on `http://localhost:8080` along with its PostgreSQL database.

<br>


## 📚 API Endpoints

All endpoints are prefixed with `/api/v1`.

| Method | Endpoint           | Description                                                                    | Authentication          | Roles Allowed            |
| :----- | :----------------- | :----------------------------------------------------------------------------- | :---------------------- | :----------------------- |
| `POST` | `/auth/google`     | Exchanges a Google ID token for a custom application JWT.                      | **Public**              | -                        |
| `POST` | `/signup`          | Creates or updates the authenticated user's profile with detailed information. | **Custom JWT Required** | Any authenticated user   |
| `GET`  | `/me`              | Retrieves the full user details for the currently authenticated user.          | **Custom JWT Required** | Any authenticated user   |
| `GET`  | `/admin/dashboard` | Example endpoint for administrators.                                           | **Custom JWT Required** | `ADMIN`                  |
| `GET`  | `/fleet/vehicles`  | Example endpoint for fleet managers.                                           | **Custom JWT Required** | `FLEET_MANAGER`, `ADMIN` |

<br>

## 🏛️ Database Schema

The service uses two main tables to store user information. The user's role is stored directly within the `app_user` table.

### `app_user` Table

Stores core user identity, role, and security information.

| Column              | Type           | Notes                                                  |
| :------------------ | :------------- | :----------------------------------------------------- |
| `id`                | `BIGSERIAL`    | **Primary Key**                                        |
| `email`             | `VARCHAR(255)` | Unique, used for login                                 |
| `role`              | `VARCHAR(255)` | The user's single role (e.g., `USER`,`FLEET`, `ADMIN`) |
| `password`          | `VARCHAR(255)` | Hashed password (for future local auth)                |
| `name`              | `VARCHAR(255)` | Full name                                              |
| `given_name`        | `VARCHAR(255)` | First name from Google                                 |
| `family_name`       | `VARCHAR(255)` | Last name from Google                                  |
| `picture`           | `VARCHAR(255)` | URL to profile picture                                 |
| `google_sub`        | `VARCHAR(255)` | Unique Google subject ID                               |
| `is_active`         | `BOOLEAN`      | Whether the user account is active                     |
| `identity_provider` | `VARCHAR(255)` | e.g., `GOOGLE`, `LOCAL`                                |
| `created_at`        | `TIMESTAMP`    | Auto-generated timestamp                               |
| `updated_at`        | `TIMESTAMP`    | Auto-updated timestamp                                 |

### `user_profile` Table

Stores additional, non-critical information about the user.

| Column                   | Type           | Notes                                |
| :----------------------- | :------------- | :----------------------------------- |
| `id`                     | `BIGINT`       | **Primary Key**, FK to `app_user.id` |
| `phone`                  | `VARCHAR(255)` | Contact phone number                 |
| `date_of_birth`          | `DATE`         | User's date of birth                 |
| `driving_license_number` | `VARCHAR(255)` | Driver's license number              |
| `passport_number`        | `VARCHAR(255)` | Optional passport number             |
| `preferred_language`     | `VARCHAR(255)` | User's language preference           |
| `country_of_residence`   | `VARCHAR(255)` | User's country of residence          |
| `created_at`             | `TIMESTAMP`    | Auto-generated timestamp             |
| `updated_at`             | `TIMESTAMP`    | Auto-updated timestamp               |

## ⚙️ Configuration

Key configuration values are managed in `application.properties`.

| Property                                               | Description                                      | Default / Example                           |
| :----------------------------------------------------- | :----------------------------------------------- | :------------------------------------------ |
| `SPRING_DATASOURCE_URL`                                | The JDBC URL for the PostgreSQL database.        | `jdbc:postgresql://db:5432/...`             |
| `SPRING_DATASOURCE_USERNAME`                           | The username for the database.                   | `exploresguser`                             |
| `SPRING_DATASOURCE_PASSWORD`                           | The password for the database user.              | `exploresgpass`                             |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | The trusted issuer for validating Google JWTs.   | `https://accounts.google.com`               |
| `spring.security.oauth2.resourceserver.jwt.audiences`  | Your Google OAuth Client ID.                     | `your-client-id.apps.googleusercontent.com` |
| `application.security.jwt.secret-key`                  | The **secret key** for signing your custom JWTs. | **Must be changed in production**           |
| `application.security.jwt.expiration`                  | The lifespan of your custom access tokens.       | `86400000` (24 hours)                       |
