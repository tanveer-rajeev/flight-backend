# Webhook Alert Config — Admin API & Frontend Guide

Admin-facing reference for configuring **outbound webhook alerts** (Discord, Slack, Microsoft Teams, generic JSON) by **alert type**.

When a configured event occurs (e.g. ticket issued but invoice post-processing fails), the backend dispatches async HTTP POST requests to every **active** matching webhook config.

Base path: `/api/admin/webhook-alert-configs`

---

## Overview

| Concept | Description |
|---------|-------------|
| **Alert type** | Business event that triggers webhooks (`TICKETED_BOOKING_POST_PROCESS_FAILED`, …) |
| **Channel** | Payload format / destination type (`DISCORD`, `SLACK`, `MICROSOFT_TEAMS`, `GENERIC_JSON`) |
| **Webhook config** | Named row: alert type + channel + webhook URL |
| **Multiple configs** | Same alert type can have many configs (e.g. two Discord channels) |

### Supported channels

| Channel | Payload |
|---------|---------|
| `DISCORD` | Discord webhook embed with `@here` mention |
| `SLACK` | `{ "text": "..." }` |
| `MICROSOFT_TEAMS` | Office 365 Connector MessageCard |
| `GENERIC_JSON` | `{ title, message, fields }` for custom receivers |

### Built-in alert types

| Value | Label | When fired |
|-------|-------|------------|
| `TICKETED_BOOKING_POST_PROCESS_FAILED` | Ticket issued but post-processing failed | Core/GDS returned ticket + `CONFIRMED`, but supplier invoice auto-create failed |
| `BOOKING_CREATE_CORE_FAILED` | Booking create core API failed | Online booking create: core HTTP error, invalid response, missing PNR, or `FAILED` status |
| `HOLD_TO_BOOK_CORE_FAILED` | Hold-to-book core API failed | Hold-to-book: core HTTP error, `FAILED` status, or non-success/non-reprice response |

Add new types in `WebhookAlertType` enum when extending for other cases.

---

## Authentication & permissions

| Action | API permission | Menu permission | Menu action |
|--------|----------------|-----------------|-------------|
| List / view / meta | `view-webhook-alert-config` | `view-webhook-alert-config-m` | `view-webhook-alert-config-ma` |
| Create | `create-webhook-alert-config` | `create-webhook-alert-config-m` | `create-webhook-alert-config-ma` |
| Update / test | `update-webhook-alert-config` | `update-webhook-alert-config-m` | `update-webhook-alert-config-ma` |
| Delete | `delete-webhook-alert-config` | `delete-webhook-alert-config-m` | `delete-webhook-alert-config-ma` |

- Admin JWT (`provider = admin`)
- Migration: `V42__webhook_alert_config.sql`
- Default `admin` role receives all permissions above

### Frontend menu integration

Load menu permissions:

```http
GET /api/permissions/menus
Authorization: Bearer <admin-token>
```

Show **Settings → Webhook Alerts** when user has `view-webhook-alert-config-m`.

| UI control | Permission slug |
|------------|-----------------|
| Page access | `view-webhook-alert-config-m` |
| View list/detail | `view-webhook-alert-config-ma` |
| Create button | `create-webhook-alert-config-ma` |
| Edit / Test button | `update-webhook-alert-config-ma` |
| Delete button | `delete-webhook-alert-config-ma` |

---

## API endpoints

### 1. Get metadata (form dropdowns)

```http
GET /api/admin/webhook-alert-configs/meta
Authorization: Bearer <admin-token>
```

**Response**

```json
{
  "success": true,
  "data": {
    "alertTypes": [
      { "value": "TICKETED_BOOKING_POST_PROCESS_FAILED", "label": "Ticket issued but post-processing failed" },
      { "value": "BOOKING_CREATE_CORE_FAILED", "label": "Booking create core API failed" },
      { "value": "HOLD_TO_BOOK_CORE_FAILED", "label": "Hold-to-book core API failed" }
    ],
    "channels": [
      { "value": "DISCORD", "label": "Discord" },
      { "value": "SLACK", "label": "Slack" },
      { "value": "MICROSOFT_TEAMS", "label": "Microsoft Teams" },
      { "value": "GENERIC_JSON", "label": "Generic JSON" }
    ]
  }
}
```

### 2. List configs

```http
GET /api/admin/webhook-alert-configs?activeOnly=true
Authorization: Bearer <admin-token>
```

### 3. Get by ID

```http
GET /api/admin/webhook-alert-configs/{id}
Authorization: Bearer <admin-token>
```

### 4. Create

```http
POST /api/admin/webhook-alert-configs
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Discord IT Alerts",
  "alertType": "TICKETED_BOOKING_POST_PROCESS_FAILED",
  "channel": "DISCORD",
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "description": "Notify IT when ticket issued but invoice fails",
  "isActive": true
}
```

### 5. Update

```http
PUT /api/admin/webhook-alert-configs/{id}
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Same body as create.

### 6. Delete

```http
DELETE /api/admin/webhook-alert-configs/{id}
Authorization: Bearer <admin-token>
```

### 7. Send test message

```http
POST /api/admin/webhook-alert-configs/{id}/test
Authorization: Bearer <admin-token>
```

Dispatches a test payload to the configured URL. Requires `update-webhook-alert-config`.

---

## Response shape

```json
{
  "id": 1,
  "name": "Discord IT Alerts",
  "alertType": "TICKETED_BOOKING_POST_PROCESS_FAILED",
  "alertTypeLabel": "Ticket issued but post-processing failed",
  "channel": "DISCORD",
  "channelLabel": "Discord",
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "description": "Notify IT when ticket issued but invoice fails",
  "isActive": true,
  "createdBy": 3,
  "updatedBy": 3,
  "createdAt": "2026-07-04T22:00:00",
  "updatedAt": "2026-07-04T22:00:00"
}
```

---

## Admin form (recommended fields)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Name | text | yes | Admin-friendly label |
| Alert type | select | yes | From `/meta` → `alertTypes` |
| Channel | select | yes | From `/meta` → `channels` |
| Webhook URL | url | yes | Must be `http` or `https` |
| Description | textarea | no | Internal note |
| Active | toggle | no | Default `true` |

**Actions:** Save, Cancel, **Send test** (calls `POST /{id}/test` after save)

---

## Example: Discord setup

Create separate configs per alert type, or use the same Discord webhook URL for multiple types.

### Ticket + invoice failure

```json
{
  "name": "IT Ticket Failure",
  "alertType": "TICKETED_BOOKING_POST_PROCESS_FAILED",
  "channel": "DISCORD",
  "webhookUrl": "https://discord.com/api/webhooks/<id>/<token>",
  "isActive": true
}
```

### Booking create core failure

```json
{
  "name": "Booking Create Failures",
  "alertType": "BOOKING_CREATE_CORE_FAILED",
  "channel": "DISCORD",
  "webhookUrl": "https://discord.com/api/webhooks/<id>/<token>",
  "isActive": true
}
```

### Hold-to-book core failure

```json
{
  "name": "Hold-to-Book Failures",
  "alertType": "HOLD_TO_BOOK_CORE_FAILED",
  "channel": "DISCORD",
  "webhookUrl": "https://discord.com/api/webhooks/<id>/<token>",
  "isActive": true
}
```

1. In Discord channel → Integrations → Webhooks → New Webhook → copy URL
2. Admin → Webhook Alerts → Create config(s) above
3. Click **Send test** on each config
4. Failures dispatch matching alert types automatically

---

## Database

Table: `webhook_alert_configs`

| Column | Description |
|--------|-------------|
| `name` | Admin label |
| `alert_type` | Event enum string |
| `channel` | `DISCORD`, `SLACK`, … |
| `webhook_url` | Outbound POST target |
| `description` | Optional note |
| `is_active` | Enable/disable without delete |

---

## Frontend checklist

- [ ] Add menu item gated by `view-webhook-alert-config-m`
- [ ] CRUD table with active filter
- [ ] Create/edit modal using `/meta` dropdowns
- [ ] Test button on edit row (`update-webhook-alert-config-ma`)
- [ ] Mask webhook URL in list (show last 8 chars) — full URL on edit only (optional UX)
- [ ] Show validation errors for invalid URL

---

## Related backend services

| Service | Role |
|---------|------|
| `WebhookAlertConfigService` | CRUD + test send |
| `WebhookAlertDispatchService` | Entry point from booking flow |
| `WebhookAlertAsyncDispatcher` | Async fan-out to active configs |
| `WebhookOutboundSender` | Channel-specific HTTP POST |
