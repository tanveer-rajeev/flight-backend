# Admin User Impersonation API

Admin-facing reference for impersonating a client user. An admin obtains a **client JWT** (`provider = user`) that acts as the target user, while the backend records which admin initiated the session.

Base path: `/api/admin/impersonation`

---

## Overview

| Item | Detail |
|------|--------|
| Purpose | Let a logged-in admin open the **client portal** as a specific user for support / troubleshooting |
| Method | `POST` |
| Auth required | Yes — **admin access token** + `admin` role or `impersonate-user` permission |
| Returns | A new **user** JWT (not a refresh token) |
| Audit | Writes to `login_history` and `activity_log` (`ADMIN_IMPERSONATE`) |

---

## Authentication

### Admin token (request)

Call this endpoint with an admin JWT from `POST /api/admin/auth/login`.

| Header | Value |
|--------|--------|
| `Authorization` | `Bearer <admin-access-token>` |
| `Content-Type` | `application/json` |

The authenticated principal must have JWT claim `provider = admin`. Non-admin tokens receive **401 Unauthorized**.

**Permission:** `impersonate-user` (assigned to global `admin` role via migration `V40__impersonate_user_permission.sql`). Users with only sub-admin roles need this permission explicitly.

### Impersonation token (response)

The returned token is a **user** token:

| JWT claim | Value |
|-----------|--------|
| `provider` | `user` |
| `subject` | Target user's email |
| `authorities` | Target user's roles |
| `isAgency` | `true` if target user is an agency account |
| `impersonatedByAdminId` | ID of the admin who started impersonation |

Use this token on **client** API routes (`/api/...`), not admin routes.

**Token lifetime**

| Target user type | TTL |
|------------------|-----|
| Regular user | 25 minutes |
| Agency user | 2 days |

There is **no refresh token** in the impersonation response. When the token expires, call impersonation again or have the user log in normally.

---

## Impersonate user

### `POST /api/admin/impersonation`

Issues a client JWT for the given user ID.

#### Request body

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `userId` | number | Yes | — | Target user ID (`users.id`) |
| `reason` | string | No | max 200 chars | Optional support note; stored in `activity_log.metadata.reason` when provided |

#### Example request

```http
POST /api/admin/impersonation HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "userId": 42,
  "reason": "Support ticket #1042 — wallet balance issue"
}
```

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Impersonation successful",
  "status": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `data.token` | string | User JWT to send on client API calls |

#### Error responses

| HTTP | When | Typical message |
|------|------|-----------------|
| 401 | Missing/invalid admin token | Unauthorized |
| 401 | Caller is not an admin (`provider != admin`) | Only admin users can impersonate. |
| 401 | Target user is deleted | User account is deleted |
| 401 | Target user is inactive | User account is not active |
| 404 | Target user ID not found | User |
| 404 | Admin record missing | Admin user |
| 400 | Validation failure (e.g. missing `userId`) | Field validation errors |

---

## Client integration flow

```mermaid
sequenceDiagram
    participant AdminUI as Admin dashboard
    participant API as Backend
    participant ClientUI as Client portal

    AdminUI->>API: POST /api/admin/auth/login
    API-->>AdminUI: admin JWT

    AdminUI->>API: POST /api/admin/impersonation { userId }
    Note over AdminUI,API: Authorization: Bearer admin JWT
    API-->>AdminUI: user JWT (impersonatedByAdminId set)

    AdminUI->>ClientUI: Open client app with user JWT
    ClientUI->>API: Client API calls
    Note over ClientUI,API: Authorization: Bearer user JWT
```

### Recommended frontend steps

1. Admin logs into the admin portal and stores the admin token separately.
2. Admin selects a user and calls `POST /api/admin/impersonation`.
3. Store the returned token as the **client session** token (e.g. separate storage key from admin token).
4. Redirect or embed the client app using the user token.
5. On expiry, either re-run impersonation or exit impersonation mode and return to admin session.

---

## Backend behaviour

On success the service:

1. Loads the admin from the JWT principal and the target user by `userId`.
2. Rejects deleted or inactive users.
3. Builds authorities from the target user's role assignments (defaults to `ROLE_user` if none).
4. Mints an impersonation JWT via `JwtUtil.generateImpersonationToken`.
5. Appends a `login_history` row with **both** `user_id` and `admin_user_id`.
6. Writes an `activity_log` row:
   - `event_type`: `ADMIN_IMPERSONATE`
   - `actor_type`: `ADMIN` (the admin who impersonated)
   - `resource_type`: `USER`, `resource_id`: target user ID
   - `metadata`: `targetUserEmail`, `targetUserName`, and `reason` (when sent in the request)
   - `ip_address`, `user_agent` from the HTTP request

### Viewing impersonation audit

```http
GET /api/admin/activity-log?eventType=ADMIN_IMPERSONATE
GET /api/admin/activity-log/admins/{adminId}
GET /api/admin/activity-log/users/{userId}
```

Requires admin role or `view-activity-log` permission.

---

## Security notes

- Endpoint is protected with `@PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'impersonate-user')")`.
- Impersonation tokens include `impersonatedByAdminId` so downstream code can detect impersonated sessions via `CustomUserDetails.getImpersonatedByAdminId()`.
- The admin's identity is **not** sent as the JWT subject; the subject is the **target user's email**. Always treat the session as the target user for authorization and data access.
- Do not log or persist the returned token in client-side analytics.

---

## Related endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/admin/auth/login` | Obtain admin JWT before impersonation |
| `POST /api/auth/login` | Normal user login (separate from impersonation) |
| `GET /api/admin/activity-log` | Query audit trail including impersonation events |

---

## Source files

| File | Role |
|------|------|
| `ImpersonationController.java` | HTTP endpoint |
| `AdminImpersonationService.java` | Business logic and audit |
| `JwtUtil.generateImpersonationToken` | JWT with `impersonatedByAdminId` |
| `ImpersonateUserRequest.java` / `ImpersonateUserResponse.java` | DTOs |
