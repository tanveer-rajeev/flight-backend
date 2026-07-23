# Admin Live Activity Feed API

Real-time ops feed for admins: bookings, wallet, and **ticket action lifecycle** events.

Base paths:

- REST snapshot / catch-up: `/api/admin/activity-feed`
- WebSocket (STOMP): `/topic/admin/activity-feed`

Related: [Activity Log Admin API](./activity-log-admin-api.md) (full audit search), [Ticket Action Admin API](./ticket-action-admin-api.md).

---

## Overview

Every eligible event is written to `activity_log` and **pushed live** to subscribed admins over WebSocket.

Default feed categories (ops-focused):

| Category | Examples |
|----------|----------|
| `BOOKING` | Created, status change, refund, reissue, void, ticket issued |
| `WALLET` | Deposit approved/rejected, balance credit/debit, credit limit |
| `TICKET_ACTION` | Submit, quote, confirm, reject, processing, finalize |

**Excluded by default:** `AUTH` (logins), `FLIGHT` (markup), `USER`, `ACCESS`, `ADMIN` / `ADMIN_ACTION` (generic controller mutations — use [Activity Log Admin API](./activity-log-admin-api.md) instead).

---

## Permissions

| Action | Permission |
|--------|------------|
| REST feed | `view-activity-log` **or** global `admin` role |
| WebSocket subscribe | Same as REST — admin JWT + (`admin` role or `view-activity-log`) |

---

## REST — initial load & reconnect

### GET `/api/admin/activity-feed`

| Query | Description |
|-------|-------------|
| `sinceId` | Return events with `id > sinceId` (catch-up after disconnect). Omit for latest snapshot. |
| `categories` | Comma-separated: `BOOKING,WALLET,TICKET_ACTION`. Default = all ops categories above. `ADMIN_ACTION` is never included in the live feed. |
| `limit` | Max events (default `50`, max `100`) |

**Initial load:**

```http
GET /api/admin/activity-feed?limit=50
```

**Reconnect catch-up:**

```http
GET /api/admin/activity-feed?sinceId=12345&limit=100
```

### Response item (`ActivityFeedEvent`)

```json
{
  "id": 12346,
  "eventType": "TICKET_ACTION_COMPLETED",
  "eventCategory": "TICKET_ACTION",
  "outcome": "SUCCESS",
  "description": "Ticket action completed",
  "summary": "Acme Travel · REISSUE completed · charge 150 USD · PNR ABC123",
  "actorType": "ADMIN",
  "actorId": 3,
  "actorEmail": "admin@example.com",
  "actorName": "Ops Admin",
  "resourceType": "TICKET_ACTION_REQUEST",
  "resourceId": "42",
  "agency": {
    "businessId": 12,
    "agencyUserId": 501,
    "agencyName": "Acme Travel",
    "agencyEmail": "agency@acme.com",
    "agencyPhone": "+8801…",
    "agencyCurrency": "BDT",
    "ownerUserId": 502,
    "ownerUserName": "Branch Agent",
    "ownerUserEmail": "agent@acme.com"
  },
  "details": {
    "bookingId": 987,
    "pnr": "ABC123",
    "ticketNo": "176-1234567890",
    "bookingReference": "BK-2026-001",
    "ticketActionRequestId": 42,
    "ticketActionType": "REISSUE",
    "ticketActionStatus": "COMPLETED",
    "amount": 150,
    "currency": "USD",
    "oldStatus": null,
    "newStatus": null
  },
  "metadata": { "...same keys as before..." },
  "traceId": "2e4d6acf-17e3-4e8c-9e15-684f75d7b4c1",
  "createdAt": "2026-07-23T06:30:00"
}
```

| Field | Description |
|-------|-------------|
| `agency` | Resolved agency/business + owner user (sub-agent when applicable) |
| `details` | Structured booking / ticket-action / wallet context for the feed row |
| `summary` | One-line text — agency name is prefixed when available |

---

## WebSocket — live push

Uses the same STOMP setup as active users and live chat.

| Item | Value |
|------|--------|
| Endpoint | `/ws` or `/api/ws` (SockJS) |
| Subscribe | `/topic/admin/activity-feed` |
| Auth header | `Authorization: Bearer {adminJwt}` |

Each new eligible `activity_log` row is broadcast as:

```json
{
  "success": true,
  "message": "Activity feed event",
  "data": { "...ActivityFeedEvent..." }
}
```

### Frontend integration

1. Connect STOMP with admin token.
2. Subscribe to `/topic/admin/activity-feed`.
3. `GET /api/admin/activity-feed?limit=50` for initial history.
4. Prepend each WebSocket message to the feed UI (newest first).
5. Track `lastEventId`; on reconnect use `?sinceId={lastEventId}`.

---

## Ticket action events

All lifecycle steps emit typed audit + live feed events:

| Event | Trigger |
|-------|---------|
| `TICKET_ACTION_SUBMITTED` | Agency `POST /api/bookings/{id}/ticket-actions` |
| `TICKET_ACTION_QUOTED` | Admin `POST /api/admin/ticket-actions/{id}/quote` |
| `TICKET_ACTION_USER_CONFIRMED` | Agency confirm quote |
| `TICKET_ACTION_REJECTED` | Admin reject, or auto-expire on quote deadline |
| `TICKET_ACTION_PROCESSING_STARTED` | Admin start processing |
| `TICKET_ACTION_COMPLETED` | Admin finalize with `resultStatus = COMPLETED` |
| `TICKET_ACTION_FAILED` | Admin finalize with `resultStatus = FAILED` |

`resourceType` = `TICKET_ACTION_REQUEST`, `resourceId` = request ID.

Metadata always includes: `ticketActionType`, `pnr`, `bookingId`, `quoteTotalAmount` (when quoted).

Reissue finalize also logs `BOOKING_REISSUED` under `BOOKING` category (booking status change).

---

## Dashboard layout (suggested)

```
┌─────────────────────────────────────────────────────────┐
│ Live Ops Feed                    [Filter: All ▼] [Pause] │
├─────────────────────────────────────────────────────────┤
│ ● REISSUE completed · charge 150 USD · PNR ABC123  2s   │
│ ● REFUND quoted · 400 USD · PNR XYZ789            45s   │
│ ● Deposit approved · 5000                         1m    │
│ ● Booking created · PNR DEF456                    2m    │
└─────────────────────────────────────────────────────────┘
```

Pair with existing `GET /api/admin/summery/dashboard-stats` for pending queue badges (deposits, ticket actions, etc.).

---

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `activity.log.enabled` | `true` | Master toggle for audit writes (feed depends on this) |

No separate toggle for WebSocket broadcast — broadcasts only when at least one admin is subscribed.

---

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| STOMP `Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]` | Server rejected **SUBSCRIBE** (auth/permission). Check server log for `STOMP SUBSCRIBE denied`. |
| `Missing permission: view-activity-log` | Sub-admin role needs `view-activity-log` permission, **or** use a user with global `admin` role. |
| `Admin token required` | Pass admin JWT on WebSocket connect (`Authorization: Bearer …` in connect headers or `?token=` on SockJS URL). |
| REST 404 on `/api/admin/activity-feed` | Restart backend after deploy — endpoint is new. |
| No live events | Something must be subscribed to `/topic/admin/activity-feed` for broadcasts; verify with a booking/ticket action after subscribe. |

Subscribe destination must be exactly **`/topic/admin/activity-feed`** (not `/app/...`).
