# Live Chat — Frontend Implementation Guide

Backend contract: [live-chat-api.md](./live-chat-api.md). This repo has no UI; implement in the **client app** and **admin portal**.

## Dependencies

```bash
npm i @stomp/stompjs sockjs-client
npm i -D @types/sockjs-client
```

## Shared STOMP client

```ts
// chatSocket.ts
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type ChatRealtimeEvent = {
  type:
    | 'CONVERSATION_CREATED'
    | 'CONVERSATION_CLAIMED'
    | 'CONVERSATION_RELEASED'
    | 'CONVERSATION_CLOSED'
    | 'MESSAGE'
    | 'TYPING'
    | 'READ';
  conversationId: number;
  status?: 'OPEN' | 'ACTIVE' | 'CLOSED';
  assignedAdminId?: number;
  assignedAdminName?: string;
  message?: ChatMessage;
  conversation?: ChatConversation;
  typing?: boolean;
  typingSenderType?: 'USER' | 'ADMIN';
  readByType?: 'USER' | 'ADMIN';
};

type BaseResponse<T> = { success: boolean; data: T; message?: string };

export function createChatClient(opts: {
  /** Origin only, e.g. http://localhost:8091 — NOT .../api */
  apiOrigin: string;
  token: string;
  onEvent: (e: ChatRealtimeEvent) => void;
  /** Admin only */
  subscribeInbox?: boolean;
}) {
  const client = new Client({
    webSocketFactory: () =>
      new SockJS(`${opts.apiOrigin}/ws?token=${encodeURIComponent(opts.token)}`),
    connectHeaders: {
      Authorization: `Bearer ${opts.token}`,
    },
    // Must match server heartbeats (10s). Missing server TaskScheduler caused connect→reconnect loops.
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 5000,
    onConnect: () => {
      // set connection = 'connected'
      client.subscribe('/user/queue/chat', (msg: IMessage) => {
        const body = JSON.parse(msg.body) as BaseResponse<ChatRealtimeEvent>;
        if (body?.data) opts.onEvent(body.data);
      });
      if (opts.subscribeInbox) {
        client.subscribe('/topic/admin/chat/inbox', (msg: IMessage) => {
          const body = JSON.parse(msg.body) as BaseResponse<ChatRealtimeEvent>;
          if (body?.data) opts.onEvent(body.data);
        });
      }
    },
    onDisconnect: () => {
      // set connection = 'reconnecting' | 'offline'
    },
    onStompError: (frame) => {
      console.error('STOMP error', frame.headers['message'], frame.body);
    },
    onWebSocketClose: (evt) => {
      console.warn('WS closed', evt.code, evt.reason);
    },
  });

  client.activate();
  // Important: create ONE client (e.g. in a context/provider). Remounting React
  // components that call activate() again causes connect ↔ reconnect flicker.

  return {
    sendMessage: (conversationId: number, body: string) =>
      client.publish({
        destination: '/app/chat/send',
        body: JSON.stringify({ conversationId, body }),
      }),
    sendTyping: (conversationId: number, typing: boolean) =>
      client.publish({
        destination: '/app/chat/typing',
        body: JSON.stringify({ conversationId, typing }),
      }),
    markRead: (conversationId: number) =>
      client.publish({
        destination: '/app/chat/read',
        body: JSON.stringify({ conversationId }),
      }),
    deactivate: () => client.deactivate(),
  };
}
```

On reconnect: REST-refetch open conversation + last message page (WS is deltas only).

### Admin: Start chat with a user

```ts
// Picker
GET /api/admin/chat/users?query=acme&page=0&size=20
// optional: &businessId=42

// Start (idempotent)
POST /api/admin/chat/conversations
{ "userId": 123, "subject": "Payment follow-up", "initialMessage": "Hi, we need docs." }
```

- Existing OPEN → auto-claimed by this admin, then optional first message  
- Existing ACTIVE → returned as-is (+ optional message)  
- New → created `ACTIVE` + assigned to admin (no claim step)  
- Events: `CONVERSATION_CREATED` / `CONVERSATION_CLAIMED` + `MESSAGE` if initialMessage set  

## Suggested state

```ts
type ChatState = {
  connection: 'connected' | 'reconnecting' | 'offline';
  conversations: Map<number, ChatConversation>;
  messagesByConversation: Map<number, ChatMessage[]>;
  typing: Map<number, boolean>;
  inboxOpenCount: number; // admin
};
```

Apply events:

| Event | UI action |
|-------|-----------|
| `MESSAGE` | Append message; bump unread if thread not focused |
| `CONVERSATION_CREATED` | Admin: insert/update inbox row |
| `CONVERSATION_CLAIMED` | User: “Agent joined”; Admin: move Open→Active |
| `CONVERSATION_CLOSED` | Disable composer; show closed banner |
| `TYPING` | Show/hide peer typing for 2s |
| `READ` | Flip `isRead` on own outbound messages |

## Client / user app

1. Floating **Chat with support** (or nav item).
2. `POST /api/chat/conversations` on open (idempotent).
3. Load messages via `GET …/messages` (page up for older).
4. Prefer WS send; if disconnected, `POST …/messages` and poll every 10s.
5. Optimistic temp id → replace when `MESSAGE` with matching body/server id arrives.
6. Debounce typing: send `typing:true` on first keystroke, `false` after ~2s idle.
7. `markRead` when thread is focused/visible.
8. Closed → read-only + **Start new chat**.

## Admin app

1. Nav item **Live chat** if permission `manage-live-chat` present.
2. Inbox tabs: Open | Active | Closed → `GET /api/admin/chat/inbox?status=…`.
3. Row: user name/email, **agency name**, assigned admin, last preview, **wait time** (`stats.waitTimeSeconds`), **duration** (`stats.durationSeconds`), message counts, status.
4. Thread header/detail sidebar: show **who chatted with whom**, `stats.activeDurationSeconds`, `stats.closedByName` on closed threads.
5. Subscribe inbox topic; prepend/update rows live; badge = open count.
6. Open thread: **Claim** when `OPEN`; composer enabled on `ACTIVE`.
7. Optional **Release**; **Close** with confirm.
8. Same typing/read/optimistic-send rules as client.

## REST helpers (sketch)

```ts
const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

export const chatApi = {
  start: (apiBase: string) =>
    fetch(`${apiBase}/api/chat/conversations`, { method: 'POST', headers }).then(r => r.json()),
  userMessages: (apiBase: string, id: number, page = 0) =>
    fetch(`${apiBase}/api/chat/conversations/${id}/messages?page=${page}&size=50`, { headers }).then(r => r.json()),
  adminInbox: (apiBase: string, status?: string, page = 0) => {
    const q = new URLSearchParams({ page: String(page), size: '20' });
    if (status) q.set('status', status);
    return fetch(`${apiBase}/api/admin/chat/inbox?${q}`, { headers }).then(r => r.json());
  },
  claim: (apiBase: string, id: number) =>
    fetch(`${apiBase}/api/admin/chat/conversations/${id}/claim`, { method: 'POST', headers }).then(r => r.json()),
};
```

## UX checklist

- [ ] Optimistic send + rollback on error
- [ ] Typing debounce
- [ ] Mark read on focus
- [ ] Reconnect refetch
- [ ] REST fallback when WS offline
- [ ] Hide admin nav without `manage-live-chat`
- [ ] Claim 409 → toast “Already claimed” + refresh inbox

## Out of scope (v1)

Bots, multi-instance Redis broker.

## Attachments, voice & internal notes

**Images** — agency or admin

1. `POST /api/files/upload/image` with `folder=live-chat` (**JPG/JPEG/PNG only**).
2. Send `attachments: [fileUrl]`. Render as `<img>`.

**Voice** — agency or admin; full guide: [live-chat-voice.md](./live-chat-voice.md)

1. Record (`MediaRecorder`, **max 60s**) → `POST /api/files/upload/audio` with `folder=live-chat` and required `durationSeconds` (≤ 60).
2. Send `attachments: [fileUrl]` (agency or admin).
3. Use `message.media[].kind === 'VOICE'` → `<audio controls>`.

**Internal notes (admin only)**

- Composer toggle **Internal note** → send with `isInternal: true`.
- Style differently; never show in user app.
- Handle WS event `INTERNAL_NOTE` on admin side only.
