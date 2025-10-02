# ExploreSG Auth Service — Local Token Implementation Summary

## Scope

Introduce first-party access and refresh tokens layered on top of the existing Google SSO onboarding flow so downstream ExploreSG services can authorize requests without depending on Google tokens.

## Highlights

- **Security tightening:** `/api/v1/**` routes now require authentication, while only the token logging utility and refresh endpoint remain public (`SecurityConfig`).
- **JWT tooling:** Added `JwtConfig` to provide an HS256 encoder and a dual-decoder (local first, Google as fallback) plus authority mapping from the `roles` claim.
- **Token lifecycle service:** Implemented `TokenService` to mint access/refresh pairs, hash refresh tokens at rest, and enforce a single active refresh token per user.
- **API additions:**
  - `POST /api/v1/auth/session` returns user context plus a token pair after Google sign-in.
  - `POST /api/v1/auth/refresh` accepts a refresh token and issues a fresh pair.
  - `POST /api/v1/signup` now responds with the persisted profile data and the newly-issued tokens.
- **DTO updates:** Added lightweight DTOs (`TokenPairResponse`, `AuthSessionResponse`, `RefreshTokenRequest`) and extended `SignupResponse` to embed the issued tokens.
- **Configuration knobs:** New properties under the `auth.jwt.*` namespace control the signing secret and expiry windows; defaults are 15 minutes for access tokens and 7 days for refresh tokens.
- **Testing:** Added `TokenServiceTests` to validate token generation, rotation, and invalidation behaviour using the in-memory test profile.

## Follow-up Considerations

- Replace the placeholder signing secret with environment-specific secrets or migrate to an asymmetric keypair for production.
- Apply role-request validation to prevent privilege escalation via `requestedRole` during signup.
- Update client applications to exchange Google tokens for local tokens and to persist/refresh them according to the new API contract.
