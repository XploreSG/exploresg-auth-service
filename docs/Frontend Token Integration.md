## Frontend Token Integration (for exploresg-auth-service)

This document explains how a frontend application should integrate with the exploresg-auth-service token setup.
It covers the token shapes, endpoints, recommended storage, security best practices, example flows, and PlantUML diagrams to visualise interactions.

### Overview

Key points:

- The backend issues an access token (short-lived) and a refresh token (longer-lived) together in a TokenPairResponse.
- Access tokens are JWTs (HS256) and include user claims (email, name, roles, user_id, identity_provider, etc.).
- Refresh tokens are opaque to the frontend (the frontend receives the raw refresh token but the server stores a hashed value).

Use-case summary table

| Purpose                  |                        Token |                   Lifetime | Frontend visibility                                           |
| ------------------------ | ---------------------------: | -------------------------: | ------------------------------------------------------------- |
| Authenticate API calls   |            accessToken (JWT) | short (default 15 minutes) | stored in secure client storage, sent in Authorization header |
| Obtain new access tokens | refreshToken (random string) |    longer (default 7 days) | stored securely, posted to /api/v1/auth/refresh               |

### DTO / API shapes

Response from endpoints that create or refresh session:

- TokenPairResponse (JSON)

{
"accessToken": "<jwt>",
"accessTokenExpiresAt": "2025-10-02T10:20:30Z",
"refreshToken": "<raw-refresh-token>",
"refreshTokenExpiresAt": "2025-10-09T09:20:30Z"

}

Endpoints used by the frontend

| Method | Path                 | Request                                        | Response                                         | Notes                                                                    |
| ------ | -------------------- | ---------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------ |
| POST   | /api/v1/auth/session | (Authenticated via Google / external identity) | AuthSessionResponse (includes TokenPairResponse) | Called after external sign-in to create local session and receive tokens |
| POST   | /api/v1/auth/refresh | { "refreshToken": "<token>" }                  | TokenPairResponse                                | Exchange a refresh token for a new access token (and new refresh token)  |
| GET    | /api/v1/me           | Authorization: Bearer <accessToken>            | User object                                      | Get user profile using access token                                      |

Note: The service supports tokens issued both locally (HS256) and tokens from Google; the local access tokens are what the frontend will use for subsequent API calls.

### Recommended Frontend Storage and Security

Industry standard recommendations (trade-offs summarised):

- Do NOT store tokens in localStorage if your app is vulnerable to XSS. Prefer an in-memory store or HttpOnly cookies.
- If you must persist the refresh token across page reloads, prefer a secure, same-site, HttpOnly cookie written by the backend. This repository currently returns the refresh token in the JSON body; to set an HttpOnly cookie you would change the backend to set Set-Cookie.
- Access tokens may be kept in memory and refreshed frequently (via refresh flow) to reduce exposure.

Short checklist:

- Store refresh token in HttpOnly, Secure, SameSite=Lax/Strict cookie when possible.
- Send access token in Authorization: Bearer <token> header on API calls.
- Protect against CSRF when using cookies (use same-site and/or CSRF token patterns).
- Rotate refresh tokens on use — the service already invalidates used refresh tokens.

Example (React) approach using HttpOnly cookie for refresh token + in-memory access token:

1. User signs in via Google. After successful sign-in, the frontend calls POST /api/v1/auth/session (authenticated with the Google token) to create a local session.
2. The backend returns TokenPairResponse. For best security, modify backend to set refresh token as an HttpOnly cookie and return only the accessToken in JSON.
3. Frontend stores accessToken in memory (React state or Redux). Use it for Authorization header on API calls.
4. When accessToken is about to expire (or on 401), call POST /api/v1/auth/refresh. If refresh token is in HttpOnly cookie, the request can be empty or include an anti-CSRF header and the backend will read the cookie.

### Sample frontend pseudo-code

Fetch wrapper that sends access token and handles refresh

```js
// ... pseudocode, adapt to your framework
let accessToken = null;

async function apiFetch(url, opts = {}) {
  const headers = opts.headers || {};
  if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
  let res = await fetch(url, { ...opts, headers });
  if (res.status === 401) {
    // try refresh
    const refreshRes = await fetch("/api/v1/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        refreshToken: getRefreshTokenFromCookieOrStore(),
      }),
    });
    if (refreshRes.ok) {
      const tokens = await refreshRes.json();
      accessToken = tokens.accessToken;
      setRefreshToken(tokens.refreshToken); // if using storage
      // retry original request
      headers["Authorization"] = `Bearer ${accessToken}`;
      res = await fetch(url, { ...opts, headers });
    } else {
      // redirect to login
      redirectToLogin();
    }
  }
  return res;
}
```

Security note: sending the refresh token in JSON body is OK if the transport is TLS and the client stores it securely. Prefer HttpOnly cookie to avoid JS access to refresh token.

### Token lifecycle and rotation (explanatory table)

| Event              | Who                                   | Effect                                                                                                                 |
| ------------------ | ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Initial sign-in    | Frontend -> POST /auth/session        | Backend creates local user (if needed) and returns TokenPairResponse; a refresh token entity is stored hashed in DB.   |
| Using access token | Frontend -> API                       | Backend verifies JWT signature and claims; returns 200 or 401.                                                         |
| Refresh            | Frontend -> POST /auth/refresh        | Backend validates hashed refresh token, revokes it, issues new TokenPairResponse, and stores new hashed refresh token. |
| Logout             | Frontend -> call API or delete cookie | Backend may revoke active tokens; frontend wipes in-memory access token and refresh cookie/local storage.              |

### PlantUML diagrams

Below are two PlantUML diagrams: (1) Sign-in / session creation, (2) Access + refresh flow.

Paste the following PlantUML code into a PlantUML editor/viewer (or use integrated IDE plugin) to render diagrams.

1. Sign-in and session creation (Google SSO -> create local session)

```
@startuml
title Sign-in and Session Creation
actor User
participant Frontend
participant Google as "Google Identity Provider"
participant Backend as "exploresg-auth-service"
database DB

User -> Frontend: Click Sign in with Google
Frontend -> Google: Redirect / popup
Google -> Frontend: ID token (JWT)
Frontend -> Backend: POST /api/v1/auth/session (Authorization: Bearer <Google-ID-Token>)
Backend -> Google: (optionally) validate ID token via issuer or introspection
Backend -> DB: upsert user from JWT
Backend -> Backend: generate TokenPair (access JWT, refresh random)
Backend -> DB: store hashed refresh token
Backend -> Frontend: 200 OK (AuthSessionResponse includes TokenPairResponse)
Frontend -> Frontend: store access token (in-memory) & refresh token (HttpOnly cookie or secure storage)

note right of Backend: Access token signed HS256; refresh token hashed in DB
@enduml
```

2. Normal API call + refresh when expired

```
@startuml
title Access call and Refresh flow
actor User
participant Frontend
participant Backend as "exploresg-auth-service"
database DB

User -> Frontend: Use app
Frontend -> Backend: API call with Authorization: Bearer <accessToken>
Backend -> Backend: verify JWT signature & claims
alt token valid
  Backend -> Frontend: 200 OK
else token expired / invalid
  Backend -> Frontend: 401 Unauthorized
  Frontend -> Backend: POST /api/v1/auth/refresh { refreshToken }
  Backend -> DB: find hashed refresh token
  alt refresh valid
    Backend -> DB: revoke stored refresh token
    Backend -> Backend: issue new TokenPair
    Backend -> DB: store hashed new refresh token
    Backend -> Frontend: 200 OK (TokenPairResponse)
  else refresh invalid
    Backend -> Frontend: 401 Unauthorized
  end
end

note right of Backend: refresh tokens are single-use and rotated on every refresh
@enduml
```

### Industry-standard considerations

- Use TLS for all requests (HTTPS).
- Prefer HttpOnly + Secure cookies for refresh tokens where possible and protect routes from CSRF.
- Keep access tokens short-lived and validate standard claims (iss, sub, exp, iat, aud if used).
- Use token revocation and rotation for refresh tokens (this project revokes used tokens).
- Log token usage events and monitor for anomalies (multiple uses of same refresh token, etc.).

### Example error workflows and handling

- 401 on API call: attempt refresh once, then redirect to login if refresh fails.
- 400/422 on refresh: clear local tokens and force re-authentication.

### What frontend developers need from backend teams

1. Clear endpoint semantics (provided above).
2. A stable JSON schema for TokenPairResponse and AuthSessionResponse.
3. Preferably: backend sets refresh token as HttpOnly cookie (Set-Cookie) and documents CSRF mitigation strategy.
4. Example token validation rules for debugging (JWT header/claims example).

### Appendix: JWT claims included by the backend

The local access JWT includes the following claims (not exhaustive):

- iss: issuer ("exploresg-auth-service")
- iat: issued at
- exp: expiry
- sub: user id or google sub
- user_id: numeric user id
- email, name, given_name, family_name, picture
- roles: array of roles (e.g., ["ROLE_USER"]) — note: JwtGrantedAuthoritiesConverter maps "roles" claim to authorities with prefix ROLE\_
- identity_provider: enum (e.g., GOOGLE or LOCAL)

---

If you'd like, I can:

- Convert the refresh flow to use HttpOnly cookies from the backend and update examples.
- Generate a small Postman collection / OpenAPI snippet for quick frontend testing.
- Create rendered PNG/SVG of the PlantUML diagrams and add them to docs.

End of document.
