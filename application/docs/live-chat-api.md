# Live Chat API

Real-time live chat between **client users** (`users`) and **admin users** (`admin_users`). Shared admin inbox: any admin with `manage-live-chat` can claim an open conversation.

Frontend integration guide: [live-chat-frontend.md](./live-chat-frontend.md).  
Voice messages (admin + client): [live-chat-voice.md](./live-chat-voice.md).

## Auth

| Audience | Auth |
|----------|------|
| User REST | JWT `provider=user`, `Authorization: Bearer …` |
| Admin REST | JWT `provider=admin` + permission `manage-live-chat` |
| WebSocket | Same JWT on handshake `?token=` or STOMP `Authorization` header |

## Conversation lifecycle

`OPEN` → (admin claims) → `ACTIVE` → (user or admin closes) → `CLOSED`

- One `OPEN` or `ACTIVE` conversation per user at a time (`POST /api/chat/conversations` returns the existing one).
- Any admin with permission may reply on `ACTIVE` (claim sets ownership for UI/metrics).
- Closed threads are read-only; user starts a new chat.

## User REST (`/api/chat`)

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/conversations` | Body optional: `{ "subject?", "initialMessage?" }` → create or return open/active |
| `GET` | `/conversations?page&size` | Own history |
| `GET` | `/conversations/{id}?includeMessages=true` | Detail; marks admin messages read |
| `GET` | `/conversations/{id}/messages?page&size` | Newest first |
| `POST` | `/conversations/{id}/messages` | `{ "body?", "attachments?": ["https://…"] }` — body or attachments required (max 5). Upload files first via `/api/files/upload/...` |
| `POST` | `/conversations/{id}/read` | Mark admin messages read |
| `POST` | `/conversations/{id}/close` | Close |

## Admin REST (`/api/admin/chat`)

Requires admin JWT (`provider=admin`). REST also accepts `ROLE_admin` / `ROLE_super_admin` or permission `manage-live-chat`.

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/users?query=&businessId=&page&size` | Start-chat picker: name/email/phone/code/agency |
| `POST` | `/conversations` | Admin starts chat: `{ "userId", "subject?", "initialMessage?" }` — reuses OPEN/ACTIVE; new → `ACTIVE` assigned to admin |
| `GET` | `/inbox?status=OPEN\|ACTIVE\|CLOSED&page&size` | Shared queue |
| `GET` | `/conversations/{id}` | Detail; marks user messages read |
| `POST` | `/conversations/{id}/claim` | Atomic `OPEN`→`ACTIVE`; **409** if already claimed |
| `POST` | `/conversations/{id}/release` | Back to `OPEN` (owner or `super_admin`) |
| `GET` | `/conversations/{id}/messages` | History |
| `POST` | `/conversations/{id}/messages` | Public reply (must be `ACTIVE`): `{ "body?", "attachments?", "isInternal": false }`. Internal note (OPEN or ACTIVE): `{ "body?", "attachments?", "isInternal": true }` — never shown to user |
| `POST` | `/conversations/{id}/read` | Mark user messages read |
| `POST` | `/conversations/{id}/close` | Close |

### Conversation session info (`stats`)

Every `ChatConversation` response (inbox, detail, WS `conversation` payload on lifecycle events) includes participant fields plus a `stats` object for admin UI panels:

| Field | Description |
|-------|-------------|
| `userFullName`, `userEmail`, `userBusinessName` | Client / agency user |
| `assignedAdminName`, `assignedAdminEmail` | Admin handling the chat |
| `stats.claimedAt` | When an admin first joined |
| `stats.firstAdminReplyAt` | First public admin message |
| `stats.durationSeconds` | Total thread length (`createdAt` → `closedAt` or now) |
| `stats.waitTimeSeconds` | User wait before admin join; for `OPEN` threads this increases until claim |
| `stats.activeDurationSeconds` | Time with an assigned admin (`claimedAt` → close or now) |
| `stats.totalMessageCount` | Public messages (user + admin) |
| `stats.userMessageCount` / `stats.adminMessageCount` | Split by sender |
| `stats.internalNoteCount` | Admin-only (omitted on user REST) |
| `stats.closedByName` | Who closed the thread |

Example inbox row snippet:

```json
{
  "id": 42,
  "userFullName": "Acme Travel",
  "userEmail": "agent@acme.com",
  "userBusinessName": "Acme Travel Ltd",
  "assignedAdminName": "Ops Admin",
  "status": "CLOSED",
  "stats": {
    "claimedAt": "2026-07-23T10:05:00",
    "durationSeconds": 1200,
    "waitTimeSeconds": 300,
    "activeDurationSeconds": 900,
    "totalMessageCount": 8,
    "userMessageCount": 5,
    "adminMessageCount": 3,
    "internalNoteCount": 1,
    "closedByName": "Ops Admin"
  }
}
```

## WebSocket (STOMP + SockJS)

- Endpoint: `{origin}/ws` or `{origin}/api/ws` (SockJS — both work)
- App prefix: `/app`
- User prefix: `/user`

> **Note:** If your HTTP `apiBase` is `http://localhost:8091/api`, do **not** append `/ws` onto that (that becomes `/api/ws`, which is fine now as an alias). Prefer `new SockJS(\`${origin}/ws?token=...\`)` where `origin` is `http://localhost:8091` (no `/api`). The SockJS `/info` probe is public and must not require a Bearer header.

### Subscribe

| Destination | Who |
|-------------|-----|
| `/user/queue/chat` | User and admin (personal events) |
| `/topic/admin/chat/inbox` | Admins with `manage-live-chat` (shared inbox) |

### Send

| Destination | Payload |
|-------------|---------|
| `/app/chat/send` | `{ "conversationId": 1, "body?", "attachments?", "isInternal?" }` |
| `/app/chat/typing` | `{ "conversationId": 1, "typing": true }` |
| `/app/chat/read` | `{ "conversationId": 1 }` |

### Event envelope

All WS payloads use `BaseResponse<ChatRealtimeEvent>`:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "type": "MESSAGE",
    "conversationId": 1,
    "status": "ACTIVE",
    "assignedAdminId": 5,
    "message": { "id": 9, "body": "Hello", "attachments": [], "isInternal": false, "senderType": "USER", "…" },
    "conversation": { "…" }
  }
}
```

`type` values: `CONVERSATION_CREATED`, `CONVERSATION_CLAIMED`, `CONVERSATION_RELEASED`, `CONVERSATION_CLOSED`, `MESSAGE`, `INTERNAL_NOTE`, `TYPING`, `READ`.

### Attachments (images + voice) — agency or admin

1. **Images:** `POST /api/files/upload/image` — JPG/JPEG/PNG only in chat. **Agency or admin JWT.**
2. **Voice:** `POST /api/files/upload/audio` — webm, ogg, mp3, m4a, wav, aac (max 5 MB, max **60 seconds**; form field `durationSeconds` required). **Agency or admin JWT.** See [live-chat-voice.md](./live-chat-voice.md).
3. Include returned `fileUrl` values in `attachments` on send (max 5). Non-agency client users cannot attach.
4. Body may be empty when at least one attachment is present.
5. Message responses include `media: [{ url, kind: "IMAGE"|"VOICE" }]` for UI.
6. Unsupported URLs are rejected with a validation error.

### Internal notes (admin only)

- Set `isInternal: true` on send.
- Visible only on admin REST/WS (`INTERNAL_NOTE` / message with `isInternal: true`).
- Never returned on user message lists; never pushed to `/user/queue/chat` for the client.
- Allowed on `OPEN` or `ACTIVE` (no claim required for notes).
- Public replies still require `ACTIVE` (after claim).

Per-user delivery uses the JWT username (**email**) as the STOMP user name.

## Offline fallback

If the peer is not present in Redis (`ActiveUserPresenceService`), an in-app `GENERAL` notification is created via `NotificationHelper`.

## Errors

| Situation | Code |
|-----------|------|
| Claim race / already claimed | `DATA_CONFLICT` (409) |
| Closed conversation send | `INVALID_STATE` (400) |
| Wrong owner | `ACCESS_DENIED` (403) |
| Missing conversation | `RESOURCE_NOT_FOUND` (404) |

## Latency & high TPS

**Why messages felt slow (before):** each send rebuilt a full conversation DTO (unread COUNT queries), and wrote offline notifications synchronously (OPEN queue even looped all admins).

**Now:**
- MESSAGE events carry a slim conversation summary (no unread COUNT on hot path)
- `saveAndFlush` so message `id` is present immediately
- Offline notifications run on `chatNotificationExecutor` (async)
- STOMP inbound/outbound pools sized for burst traffic (16–64 threads)

**Frontend:** show messages **optimistically** on send; reconcile with WS `MESSAGE` / REST ACK. Do not wait for REST if using `/app/chat/send`.

**Scale-out note:** in-memory STOMP broker is single-node. For multi-instance high TPS use a shared broker (Redis/Rabbit) later.
