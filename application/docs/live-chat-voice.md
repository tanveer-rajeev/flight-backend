# Live Chat — Voice Messages (Admin + Client)

Voice notes use the same send path as text/images: upload audio → put URL in `attachments` → REST or STOMP send.

## Backend contract

### Upload

```
POST /api/files/upload/audio
Content-Type: multipart/form-data
Authorization: Bearer <user-or-admin-jwt>

file: <Blob>
folder: live-chat          (default)
durationSeconds: <number>  (required, max 60)
```

**Allowed:** `webm`, `ogg`, `mp3`, `m4a`, `wav`, `aac` (max **5 MB**, max **60 seconds / 1 minute**)  
`durationSeconds` is **required** and must be `> 0` and `≤ 60`.  
**Who:** **agency or admin**. Non-agency client users cannot upload.  
Content-Type may include codec params (e.g. `audio/webm;codecs=opus`) — server normalizes these.  
Blob uploads without a filename get a proper extension inferred from content-type.

**Form fields:** `file` (multipart), `durationSeconds` (recording length in seconds).

**Response:** `{ data: { fileUrl, fileName, fileSize, type: "audio" } }`

```ts
const fd = new FormData();
fd.append('file', blob, 'note.webm'); // give a real filename when possible
fd.append('folder', 'live-chat');
fd.append('durationSeconds', String(Math.ceil(durationSec))); // required, ≤ 60
```

Images: `POST /api/files/upload/image` (JPG/JPEG/PNG for chat) — **agency or admin**, same auth rule.

### Send (same for admin + client)

```json
POST /api/chat/conversations/{id}/messages
POST /api/admin/chat/conversations/{id}/messages
// or STOMP /app/chat/send

{
  "body": "",
  "attachments": ["https://…/live-chat/audio/uuid.webm"]
}
```

Message DTO includes:

```json
{
  "attachments": ["https://…/note.webm"],
  "media": [{ "url": "https://…/note.webm", "kind": "VOICE" }]
}
```

`kind` is `IMAGE` | `VOICE`. Prefer `media` in the UI.

---

## Client app (agency)

1. Hold-to-record (or tap) using `MediaRecorder` → typically `audio/webm` or `audio/ogg`.
2. On stop (or auto-stop at 60s) → upload with `durationSeconds` (≤ 60).
3. Send chat message with `attachments: [fileUrl]` (body optional).
4. Prefer STOMP `/app/chat/send` for speed; optimistic bubble with local blob URL, replace when WS `MESSAGE` arrives.
5. Render: if `media[].kind === 'VOICE'` → `<audio controls src={url} />`.

```ts
async function uploadVoice(
  apiBase: string,
  token: string,
  blob: Blob,
  durationSeconds: number,
  filename = 'note.webm',
) {
  const fd = new FormData();
  fd.append('file', blob, filename);
  fd.append('folder', 'live-chat');
  fd.append('durationSeconds', String(Math.ceil(durationSeconds)));
  const res = await fetch(`${apiBase}/files/upload/audio`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: fd,
  });
  if (!res.ok) throw new Error(await res.text());
  const json = await res.json();
  return json.data.fileUrl as string;
}

const MAX_VOICE_SEC = 60;

// Record (browser) — auto-stop at 60s
const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
const chunks: BlobPart[] = [];
const startedAt = Date.now();
let autoStop: ReturnType<typeof setTimeout> | undefined;

recorder.ondataavailable = (e) => chunks.push(e.data);
recorder.onstop = async () => {
  clearTimeout(autoStop);
  stream.getTracks().forEach((t) => t.stop());
  const durationSeconds = (Date.now() - startedAt) / 1000;
  if (durationSeconds > MAX_VOICE_SEC) {
    // should not happen if auto-stop is set; still guard
    return;
  }
  const blob = new Blob(chunks, { type: 'audio/webm' });
  const url = await uploadVoice(API_BASE, token, blob, durationSeconds);
  client.publish({
    destination: '/app/chat/send',
    body: JSON.stringify({ conversationId, attachments: [url] }),
  });
};
recorder.start();
autoStop = setTimeout(() => {
  if (recorder.state === 'recording') recorder.stop();
}, MAX_VOICE_SEC * 1000);
// … user can stop earlier with recorder.stop();
```

UX tips:
- Show recording timer; **hard cap at 60 seconds** (server rejects longer).
- Auto-stop `MediaRecorder` at 60s and send `durationSeconds`.
- Mic permission denied → clear error.
- iOS Safari: prefer `audio/mp4` / `.m4a` if `webm` unsupported.

---

## Admin app

Same upload + send as agency (images and voice, max 60s). Internal notes may also include attachments (`isInternal: true`).

```ts
const fileUrl = await uploadVoice(API_BASE, adminToken, blob, durationSeconds, 'note.webm');

await fetch(`${API_BASE}/admin/chat/conversations/${id}/messages`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${adminToken}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ attachments: [fileUrl] }),
});
```

Inbox preview: last message may show `[voice]` (server sets `lastMessagePreview`).

---

## Rendering helper

```ts
function renderMedia(media: { url: string; kind: 'IMAGE' | 'VOICE' }[]) {
  return media.map((m) =>
    m.kind === 'VOICE'
      ? <audio key={m.url} controls preload="metadata" src={m.url} />
      : <img key={m.url} src={m.url} alt="" />
  );
}
```

---

## Checklist

**Client (agency)**
- [ ] Mic record + stop; auto-stop at **60s**
- [ ] Upload `/api/files/upload/audio` with `durationSeconds` (agency JWT)
- [ ] Send with `attachments`
- [ ] Play `kind === 'VOICE'`
- [ ] Optimistic UI

**Admin**
- [ ] Same record/upload/send (max 1 min)
- [ ] Inbox shows `[voice]` preview
- [ ] Works on claimed ACTIVE threads

**Not in scope**
- Live voice calls (WebRTC) — separate feature
