# Activity Log Admin API

Admin-facing reference for querying the unified audit trail (`activity_log` table).

Base path: `/api/admin/activity-log`

---

## Overview

The activity log records security and admin actions with explicit **actor type** (`USER`, `ADMIN`, `SYSTEM`, `GUEST`) so `users.id` and `admin_users.id` are never ambiguous.

Authenticated controller mutations (POST/PUT/PATCH/DELETE on non-public routes) are automatically logged as `ADMIN_ACTION` via `ControllerMutationAuditAspect`, unless the endpoint is annotated with `@SkipAutoAudit`, `@AuditedAction`, or already covered by a domain audit support class.

| Item | Detail |
|------|--------|
| Auth | Admin JWT (`provider = admin`) |
| Permission | `admin` role **or** `view-activity-log` |
| Migrations | `V38__activity_log.sql`, `V39__activity_log_permission.sql` |

---

## Events currently logged

### Authentication (`AUTH`)

| Event | Trigger |
|-------|---------|
| `USER_LOGIN` | Client login, OAuth login |
| `ADMIN_LOGIN` | Admin login |
| `LOGIN_FAILED` | Failed client/admin login |
| ~~`TOKEN_REFRESH`~~ | *(disabled)* Client or admin refresh token |
| `ADMIN_IMPERSONATE` | `POST /api/admin/impersonation` |
| `PASSWORD_RESET` | Client/admin password reset |
| `PASSWORD_CHANGE` | Admin password change |

### Access (`ACCESS`)

| Event | Trigger |
|-------|---------|
| `ROLE_ASSIGNED` | `POST /api/roles/assign-to-users` |
| `PERMISSION_ASSIGNED` | `POST /api/roles/assign-permissions` |

### Wallet (`WALLET`)

| Event | Trigger |
|-------|---------|
| `DEPOSIT_APPROVED` / `DEPOSIT_REJECTED` | `POST /api/wallet/approvals/{depositId}` |
| `CREDIT_LIMIT_CHANGED` | `POST /api/admin/credit-limit/grant` |
| `CREDIT_REQUEST_APPROVED` / `CREDIT_REQUEST_REJECTED` | Credit request approve/reject |

### User / business (`USER`)

| Event | Trigger |
|-------|---------|
| `BUSINESS_APPROVED` | `PUT /api/admin/businesses/{businessId}/approve` |

### Flight / markup (`FLIGHT`)

| Event | Trigger |
|-------|---------|
| `MARKUP_PLAN_CREATED` / `UPDATED` / `DELETED` | Markup plan CRUD |
| `MARKUP_RULE_CREATED` / `UPDATED` / `DELETED` | Markup rule CRUD |

### Booking (`BOOKING`)

| Event | Trigger |
|-------|---------|
| `BOOKING_CREATED` | `POST /api/booking/create`, `/create-manual`, `/import-pnr`, `/group-ticket-manual` (via `BookingService.create`) |
| `BOOKING_STATUS_CHANGED` | Generic status updates (`PUT /{id}/status`, `/admin/status`, `/status-ticket`, etc.) |
| `BOOKING_CANCELLED` | Status transition to `CANCELLED` (e.g. `POST /cancel-booking`) |
| `BOOKING_REFUNDED` | Status transition to `REFUND` (e.g. `POST /{id}/admin/refund`, refund status updates) |
| `TICKET_ISSUED` | Status transition to `TICKET_ISSUED` / `TICKETED` (e.g. `POST /issue-ticket`) |
| `BOOKING_DELETED` | `DELETE /{id}` |
| `BOOKING_UPDATED` | `PUT /{id}` (field update) |

`resourceType` is `BOOKING`; `resourceId` is the booking ID. Metadata includes `pnr`, `oldStatus`, `newStatus`, and optional `reason`. Admin refunds also include `refundType`, `deductionAmount`, and `refundedAmount`.

### Wallet (`WALLET`) — service-level

| Event | Trigger |
|-------|---------|
| `BALANCE_DEBIT` | `POST /api/wallet/admin/charge`, `/service-deduct` |
| `BALANCE_CREDIT` | `POST /api/wallet/admin/deposit` |
| `DEPOSIT_APPROVED` / `DEPOSIT_REJECTED` | `POST /api/wallet/approvals/{depositId}` |
| `CREDIT_LIMIT_CHANGED` | `POST /api/admin/credit-limit/grant` |
| `CREDIT_REQUEST_APPROVED` / `CREDIT_REQUEST_REJECTED` | Credit request approve/reject |

### Catch-all (`ADMIN`)

| Event | Trigger |
|-------|---------|
| `ADMIN_ACTION` | Any authenticated non-public controller mutation not covered above (CMS, users, visa, tour, notifications, etc.) |

Read-like POST endpoints (`/filter`, `/search`, `/get-reservation`, `/load-booking`, `/heartbeat`) are excluded. Public routes (`/api/auth/**`, `/api/flights/**`, payment callbacks, etc.) are excluded.

---

## Search activity logs

### `GET /api/admin/activity-log`

#### Query parameters

| Param | Type | Description |
|-------|------|-------------|
| `actorType` | `USER`, `ADMIN`, `SYSTEM`, `GUEST` | Filter by actor entity type |
| `actorId` | number | Actor ID within that type |
| `eventCategory` | `AUTH`, `ACCESS`, `WALLET`, `BOOKING`, `FLIGHT`, `ADMIN`, `USER`, `SYSTEM` | Category filter |
| `eventType` | enum | e.g. `ADMIN_IMPERSONATE`, `DEPOSIT_APPROVED` |
| `resourceType` | string | e.g. `USER`, `WALLET_DEPOSIT`, `BUSINESS` |
| `resourceId` | string | Resource identifier |
| `traceId` | string | Correlate with app logs / error logs |
| `from` | ISO date-time | Start of time range |
| `to` | ISO date-time | End of time range |
| `page` | int | Zero-based page (default `0`) |
| `size` | int | Page size 1–200 (default `50`) |

#### Example

```http
GET /api/admin/activity-log?eventType=ADMIN_IMPERSONATE&page=0&size=20
Authorization: Bearer <admin-jwt>
```

#### Response fields (per row)

| Field | Description |
|-------|-------------|
| `actorType` / `actorId` / `actorEmail` / `actorName` | Who performed the action |
| `impersonatedByAdminId` / `impersonatedByAdminName` | Set on user JWT sessions started via impersonation |
| `resourceType` / `resourceId` | Target entity |
| `metadata` | JSON string with event-specific details |
| `traceId` | Request correlation ID |
| `ipAddress` / `userAgent` | Client context when available |
| `createdAt` | Event timestamp (user timezone applied when header set) |

---

## Convenience endpoints

```http
GET /api/admin/activity-log/users/{userId}?page=0&size=50
GET /api/admin/activity-log/admins/{adminId}?page=0&size=50
GET /api/admin/activity-log/{id}
```

---

## Configuration

```properties
activity.log.enabled=true
```

When `false`, new audit rows are skipped (existing data unchanged).

---

## Related docs

- [Admin User Impersonation API](./admin-impersonation-api.md)
