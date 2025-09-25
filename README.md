# Explore Singapore Auth Service

A robust, secure authentication service built with Express.js, TypeScript, and PostgreSQL for the Explore Singapore platform.

## 🚀 Features

- **User Registration & Login**: Email/password and Google SSO authentication
- **Email Verification**: Secure email verification system
- **JWT Authentication**: Access and refresh token management
- **Rate Limiting**: Protection against brute force attacks
- **Input Validation**: Comprehensive request validation
- **Security Hardening**: Helmet, CORS, and other security middlewares
- **Error Handling**: Centralized error handling with logging
- **Database Integration**: PostgreSQL with connection pooling
- **Testing**: Unit tests with Jest
- **TypeScript**: Full type safety and modern ES features

## 🛠 Quick Start

1. **Install dependencies:**

   ```bash
   npm install
   ```

2. **Setup environment:**

   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

## 📚 API Endpoints

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/verify-email` - Email verification
- `GET /api/v1/auth/me` - Get user profile
- `POST /api/v1/auth/refresh-token` - Refresh JWT token
- `GET /api/v1/health` - Health check

## 🏗 Architecture

Following Domain-Driven Design (DDD) and Clean Architecture principles:

```
src/
├── config/          # Configuration
├── controllers/     # Request handlers
├── middleware/      # Express middlewares
├── repositories/    # Data access layer
├── routes/          # API routes
├── services/        # Business logic
├── types/           # TypeScript types
├── utils/           # Utilities
└── tests/           # Unit tests
```

## 🔐 Security Features

- Password hashing with bcrypt
- JWT access & refresh tokens
- Rate limiting protection
- Input validation with Joi
- Security headers with Helmet
- CORS protection

See UC-001.md for detailed use case documentation.
