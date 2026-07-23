# Admin Auth API

Admin-facing reference for registration, login (with OTP), password management, and token refresh.

Base path: `/api/admin/auth`

All routes under this path are **public** (no bearer token required), except `POST /change-password`.

---

## Overview

| Item | Detail |
|------|--------|
| Purpose | Authenticate admin users and issue JWT access + refresh tokens |
| Login | Two-step: send OTP after credential check, then login with email + password + OTP |
| JWT provider claim | `admin` |
| OTP delivery | Email (6-digit code, 5-minute expiry) |
| OTP resend throttle | 2 minutes between sends (registration / forgot-password only) |
| Login OTP reuse | Any unused, non-expired code works; no new email if one is still valid |
| Audit | Failed logins → `LOGIN_FAILED`; successful login → `ADMIN_LOGIN` + `login_history` row |

---

## Login flow (OTP)

Admin login requires a one-time code sent to the admin's registered email.

```mermaid
sequenceDiagram
    participant UI as Admin login form
    participant API as Backend
    participant Mail as Email

    UI->>API: POST /send-login-otp { email, password }
    API->>API: Validate credentials
    API->>Mail: Send 6-digit OTP
    API-->>UI: 200 OTP sent

    UI->>API: POST /login { email, password, otp }
    API->>API: Validate credentials + OTP
    API-->>UI: 200 { token, refreshToken }
```

### Recommended frontend steps

1. User enters **email** and **password**.
2. Call `POST /send-login-otp` if the user does not already have a valid code (or `POST /resend-login-otp` after expiry).
3. User enters the **OTP** from email.
4. Call `POST /login` with email, password, and OTP — works with **any unused code still within the 5-minute window**, including one from an earlier send.
5. Store `data.token` (access) and `data.refreshToken` for subsequent admin API calls.
6. On access token expiry, call `POST /refresh-token` with the refresh token.

---

## Send login OTP

### `POST /api/admin/auth/send-login-otp`

Validates email and password, then emails a login OTP when needed. Does **not** return tokens.

If the admin already has an **unused, non-expired** OTP, the call succeeds immediately and **does not send another email** — the user should enter the code they already received.

#### Request body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | Admin email |
| `password` | string | Yes | Admin password |

#### Example request

```http
POST /api/admin/auth/send-login-otp HTTP/1.1
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "secret-password"
}
```

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "OTP sent to your email.",
  "status": 200,
  "data": null
}
```

#### Error responses

| HTTP | When | Typical message |
|------|------|-----------------|
| 404 | Email not found | User |
| 401 | Account not verified | Account not verified! |
| 401 | Account inactive | Account is not active! |
| 400 | Wrong password | Invalid password |
| 400 | Validation failure | Field validation errors |

Note: No `409` throttle on login OTP when a valid unused code already exists. A new email is sent only after the previous code expired or was used.

---

## Resend login OTP

### `POST /api/admin/auth/resend-login-otp`

Same behaviour as `send-login-otp`. If a valid unused OTP still exists, returns success without sending a new email. Otherwise validates credentials and sends a fresh code.

#### Request body

Same as [Send login OTP](#send-login-otp).

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "OTP resent to your email.",
  "status": 200,
  "data": null
}
```

---

## Login

### `POST /api/admin/auth/login`

Completes admin login. Accepts **any unused OTP that has not expired**, not only the most recently sent code.

#### Request body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | Admin email |
| `password` | string | Yes | Admin password |
| `otp` | string | Yes | 6-digit code from email |

#### Example request

```http
POST /api/admin/auth/login HTTP/1.1
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "secret-password",
  "otp": "123456"
}
```

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Login successful",
  "status": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "a1b2c3d4-e5f6-..."
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `data.token` | string | Admin access JWT (`provider = admin`) |
| `data.refreshToken` | string | Opaque refresh token for `/refresh-token` |

#### Error responses

| HTTP | When | Typical message |
|------|------|-----------------|
| 404 | Email not found | User |
| 401 | Account not verified | Account not verified! |
| 401 | Account inactive | Account is not active! |
| 400 | Wrong password | Invalid password |
| 401 | Wrong or used OTP | Invalid OTP |
| 401 | OTP older than 5 minutes | OTP expired! |
| 400 | Missing OTP | OTP is required |

#### Using the access token

| Header | Value |
|--------|--------|
| `Authorization` | `Bearer <admin-access-token>` |
| `Content-Type` | `application/json` |

---

## Refresh access token

### `POST /api/admin/auth/refresh-token`

Issues a new access token using a valid admin refresh token.

#### Request body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | Yes | Refresh token from login response |

#### Example request

```http
POST /api/admin/auth/refresh-token HTTP/1.1
Content-Type: application/json

{
  "refreshToken": "a1b2c3d4-e5f6-..."
}
```

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "status": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

Note: refresh returns a new **access token only**; the same refresh token remains valid until it expires or is revoked.

---

## Registration (account verification)

New admin accounts must verify email via OTP before they can log in.

### `POST /api/admin/auth/register`

Creates an unverified admin user and sends a verification OTP.

#### Request body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | Admin email |
| `password` | string | Yes | Password |
| `fullName` | string | Yes | Display name |
| `phone` | string | Yes | Required by DTO validation |
| `currencyCode` | string | Yes | Required by DTO validation |

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Registration successful. OTP sent to your email.",
  "status": 200,
  "data": null
}
```

### `POST /api/admin/auth/verify-otp`

Marks the admin account as verified after registration.

#### Request body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | Registered email |
| `otp` | string | Yes | Verification code from email |

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "OTP verified. You can now log in.",
  "status": 200,
  "data": null
}
```

After verification, use the [login flow](#login-flow-otp) to sign in.

---

## Forgot / reset password

### `POST /api/admin/auth/forgot-password`

Sends a password-reset OTP to the admin email.

#### Request body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | Admin email |

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "OTP sent to your email. Please check your inbox.",
  "status": 200,
  "data": null
}
```

### `POST /api/admin/auth/reset-password`

Sets a new password after OTP verification.

#### Request body

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | string | Yes | valid email | Admin email |
| `newPassword` | string | Yes | min 6 chars | New password |
| `otp` | string | Yes | — | Reset code from email |

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Password reset successful. You can now log in with your new password.",
  "status": 200,
  "data": null
}
```

---

## Change password (authenticated)

### `POST /api/admin/auth/change-password`

Changes password for the currently logged-in admin.

**Auth required:** admin access token.

#### Request body

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `oldPassword` | string | Yes | — | Current password |
| `newPassword` | string | Yes | min 6 chars | New password |

#### Example request

```http
POST /api/admin/auth/change-password HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "oldPassword": "current-password",
  "newPassword": "new-secure-password"
}
```

#### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Password changed successfully.",
  "status": 200,
  "data": null
}
```

---

## OTP rules (all flows)

| Rule | Value |
|------|--------|
| Code format | 6 digits |
| Expiry | 5 minutes |
| Resend cooldown | 2 minutes (registration / forgot-password only) |
| Login reuse | Unused codes remain valid until expiry; login accepts any of them |
| Single use | Each OTP is invalidated after successful login or verification |
| Delivery | Email via configured mail service |

Login OTPs are independent from registration and password-reset OTPs, but all are stored in `otp_tokens` and matched per admin user.

---

## Backend behaviour (login)

On successful `POST /login`, the service:

1. Validates email, password, verified flag, and active status.
2. Validates and consumes the login OTP.
3. Loads role assignments and builds JWT authorities (`ROLE_<slug>`).
4. Mints admin access JWT (`provider = admin`).
5. Creates a refresh token (IP + user-agent tracked).
6. Writes `login_history` and `activity_log` (`ADMIN_LOGIN`).
7. Marks the admin as online in the presence service.

Failed credential or OTP attempts during login or send-OTP are recorded as `LOGIN_FAILED` audit events.

---

## Related docs

| Doc | Use |
|-----|-----|
| [Admin User Impersonation API](./admin-impersonation-api.md) | Requires admin JWT from this login flow |
| [Activity Log Admin API](./activity-log-admin-api.md) | `ADMIN_LOGIN` / `LOGIN_FAILED` event details |
