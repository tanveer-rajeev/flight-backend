# R2 File Upload & Passport Extraction API

Frontend implementation reference for the Cloudflare R2 file upload manager.

> **Passport extraction** is documented separately in [passport-extraction-api.md](./passport-extraction-api.md) (Tesseract OCR, `POST /api/passport/extract`).

---

## Overview

Two feature sets share the same authentication model (JWT Bearer) and response envelope (`BaseResponse<T>`):

| Feature | Base path | Purpose |
|---------|-----------|---------|
| **File upload manager** | `/api/r2/files` | Upload images & documents to Cloudflare R2; delete by URL |
| **Passport extraction** | `/api/passport` | See [passport-extraction-api.md](./passport-extraction-api.md) |

All endpoints require a valid **`Authorization: Bearer <jwt>`** header. No public/anonymous access.

---

## Authentication

Every request must include:

```http
Authorization: Bearer <access_token>
Content-Type: multipart/form-data   (for file uploads)
```

On `401` the server returns:

```json
{ "success": false, "status": 401, "message": "Unauthorized" }
```

---

## Common response envelope

```json
{
  "success": true,
  "status": 200,
  "message": "...",
  "data": { ... }
}
```

Error shape:

```json
{
  "success": false,
  "status": 400,
  "message": "Invalid image type. Allowed: image/png, image/jpeg, ...",
  "error": { ... }
}
```

---

## File Upload Manager

### POST `/api/r2/files/upload/image`

Upload an image file. Returns the public Cloudflare R2 URL.

**Accepted types:** `image/png`, `image/jpeg`, `image/jpg`, `image/gif`, `image/webp`  
**Max size:** 10 MB

#### Request (multipart/form-data)

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `file` | File | Yes | — | Image file |
| `folder` | string | No | `general` | Logical folder prefix in R2 (e.g. `avatars`, `tour-covers`) |

#### Example

```js
const form = new FormData();
form.append('file', imageFile);
form.append('folder', 'avatars');

const res = await fetch('/api/r2/files/upload/image', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` },
  body: form,
});
const { data } = await res.json();
// data.fileUrl  → "https://pub-xxx.r2.dev/avatars/images/uuid.jpg"
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Image uploaded successfully",
  "data": {
    "fileUrl": "https://pub-xxx.r2.dev/avatars/images/550e8400-e29b-41d4-a716-446655440000.jpg",
    "fileName": "profile.jpg",
    "fileSize": 204800,
    "uploadedBy": "user@example.com",
    "type": "image"
  }
}
```

| Field | Description |
|-------|-------------|
| `fileUrl` | Full public CDN URL — use this as the `src` for `<img>` or save to the DB |
| `fileName` | Original filename supplied by the browser |
| `fileSize` | Bytes |
| `uploadedBy` | JWT subject (email / username) |
| `type` | Always `"image"` for this endpoint |

---

### POST `/api/r2/files/upload/document`

Upload a document. Returns the public R2 URL.

**Accepted types:** PDF · Word (doc/docx) · Excel (xls/xlsx) · PowerPoint (ppt/pptx) · CSV · plain text · `application/octet-stream` (generic binary fallback)  
**Max size:** 20 MB

#### Request (multipart/form-data)

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `file` | File | Yes | — | Document file |
| `folder` | string | No | `general` | Logical folder prefix in R2 |

#### Example

```js
const form = new FormData();
form.append('file', pdfFile);
form.append('folder', 'visa-applications');

const res = await fetch('/api/r2/files/upload/document', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` },
  body: form,
});
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Document uploaded successfully",
  "data": {
    "fileUrl": "https://pub-xxx.r2.dev/visa-applications/documents/uuid.pdf",
    "fileName": "visa-application.pdf",
    "fileSize": 1048576,
    "uploadedBy": "agent@tufantrip.com",
    "type": "document"
  }
}
```

---

### DELETE `/api/r2/files/delete`

Delete a previously uploaded file by its public URL.

#### Request (query param)

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `fileUrl` | string | Yes | Full R2 public URL returned by an upload endpoint |

#### Example

```js
const url = encodeURIComponent('https://pub-xxx.r2.dev/avatars/images/uuid.jpg');
const res = await fetch(`/api/r2/files/delete?fileUrl=${url}`, {
  method: 'DELETE',
  headers: { Authorization: `Bearer ${token}` },
});
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "File deleted successfully",
  "data": {
    "deletedBy": "admin@tufantrip.com",
    "key": "avatars/images/550e8400-e29b-41d4-a716-446655440000.jpg"
  }
}
```

| Field | Description |
|-------|-------------|
| `key` | R2 object key that was deleted |
| `deletedBy` | JWT subject of the caller |

#### Validation errors

| Condition | Message |
|-----------|---------|
| `fileUrl` doesn't match the configured R2 bucket | `Invalid or unrecognised R2 file URL` |
| File already deleted / not found | R2 silently succeeds (idempotent) |

---

## Passport Extraction

### POST `/api/passport/extract`

Uploads a passport image to Cloudflare R2, sends it to DeepSeek's vision model, and returns the extracted passport fields along with the R2 image URL.

**Accepted types:** `image/jpeg`, `image/jpg`, `image/png`  
**Max size:** 10 MB  
**Storage path:** `passports/<uuid>.jpg` in the R2 bucket

#### Request (multipart/form-data)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | Passport scan or clear photo (JPEG / PNG) |

#### Example

```js
const form = new FormData();
form.append('file', passportImageFile);

const res = await fetch('/api/passport/extract', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` },
  body: form,
});
const { data, message } = await res.json();
```

#### Response — successful extraction

```json
{
  "success": true,
  "status": 200,
  "message": "Passport data extracted successfully",
  "data": {
    "imageUrl": "https://pub-xxx.r2.dev/passports/550e8400-e29b-41d4-a716-446655440000.jpg",
    "surname": "SMITH",
    "givenNames": "JOHN WILLIAM",
    "passportNumber": "A12345678",
    "nationality": "USA",
    "dateOfBirth": "15 JAN 1990",
    "dateOfExpiry": "14 JAN 2030",
    "dateOfIssue": "15 JAN 2020",
    "gender": "M",
    "placeOfBirth": "NEW YORK",
    "issuingCountry": "United States of America",
    "mrzLine1": "P<USASMITH<<JOHN<WILLIAM<<<<<<<<<<<<<<<<<<<<",
    "mrzLine2": "A123456789USA9001151M3001149<<<<<<<<<<<<<<<6",
    "extracted": true
  }
}
```

#### Response — image saved but extraction unstructured

When the AI returns text that cannot be parsed as JSON (e.g. very low quality scan):

```json
{
  "success": true,
  "status": 200,
  "message": "Image uploaded but extraction returned unstructured data — see rawExtraction",
  "data": {
    "imageUrl": "https://pub-xxx.r2.dev/passports/uuid.jpg",
    "extracted": false,
    "rawExtraction": "I can see a passport but the image quality is too low to read the fields clearly..."
  }
}
```

**The image is always uploaded to R2 regardless of extraction outcome.** Use `imageUrl` to display or link the stored scan.

#### Response field reference

| Field | Type | Description |
|-------|------|-------------|
| `imageUrl` | string | Permanent R2 URL of the stored passport image |
| `surname` | string \| null | Family name (uppercase) |
| `givenNames` | string \| null | Given / first + middle names |
| `passportNumber` | string \| null | Document number |
| `nationality` | string \| null | 3-letter ISO or full country |
| `dateOfBirth` | string \| null | Raw text from passport (e.g. `15 JAN 1990`) |
| `dateOfExpiry` | string \| null | Expiry date raw text |
| `dateOfIssue` | string \| null | Issue date raw text |
| `gender` | string \| null | `M`, `F`, or `X` |
| `placeOfBirth` | string \| null | City / country of birth |
| `issuingCountry` | string \| null | Full issuing country name |
| `mrzLine1` | string \| null | MRZ top line (44 chars) |
| `mrzLine2` | string \| null | MRZ bottom line (44 chars) |
| `extracted` | boolean | `true` = fields parsed; `false` = see `rawExtraction` |
| `rawExtraction` | string \| null | Raw AI text — only present when `extracted` is `false` |

#### Validation errors

| Condition | Message |
|-----------|---------|
| No file or empty file | `Passport image file cannot be empty` |
| Not JPEG or PNG | `Passport image must be JPEG or PNG` |
| File > 10 MB | `Passport image too large. Maximum size is 10 MB` |
| DeepSeek API unreachable | `500 — DeepSeek API call failed: ...` |

---

## UI implementation notes

### File upload component

1. Use an `<input type="file" accept="image/*">` (or `accept=".pdf,.doc,.docx,.csv"` for documents).
2. Show a file size warning client-side before submitting (10 MB for images, 20 MB for documents).
3. On success, persist `data.fileUrl` — this is the only reference to the stored file. The backend stores no database record for generic uploads.
4. For **delete**, pass the exact `fileUrl` value back; do not reconstruct or shorten it.
5. Show a loading/spinner during upload — large files may take several seconds.
6. Display `data.uploadedBy` and `data.fileSize` in a confirmation toast if needed.

### Passport extraction component

1. Accept only JPEG/PNG with a 10 MB cap — validate client-side with `file.type` and `file.size`.
2. Show a progress indicator — the endpoint uploads to R2 **and** calls an external AI API, so it may take 5–15 seconds.
3. Always display `data.imageUrl` as a thumbnail — even when `extracted` is `false`.
4. When `extracted` is `true`, pre-fill a traveller form with the returned fields. All dates are raw strings (not ISO); parse them as needed in the UI (e.g. `"15 JAN 1990"` → `Date`).
5. When `extracted` is `false`, show the `rawExtraction` text in a read-only textarea so the user can manually copy the values.
6. Make all extracted fields editable — AI extraction is a best-effort assistant, not a trusted authority.

### Sample passport extraction form layout

```
[ 📎 Upload passport image ]   ← file input, JPEG/PNG only, max 10 MB

┌─────────────────────────────────────────────┐
│  [ Passport thumbnail ]                     │
│  Stored at: https://pub-xxx.r2.dev/...      │
└─────────────────────────────────────────────┘

Extracted data  (edit before saving)
  Surname:          [SMITH              ]
  Given names:      [JOHN WILLIAM       ]
  Passport no.:     [A12345678          ]
  Nationality:      [USA                ]
  Date of birth:    [15 JAN 1990        ]
  Date of expiry:   [14 JAN 2030        ]
  Gender:           [M                  ]
  Place of birth:   [NEW YORK           ]
  Issuing country:  [United States ...  ]

MRZ
  Line 1:  [P<USASMITH<<JOHN<WILLIAM<<<<<<<<<<<<<<<<<<<<]
  Line 2:  [A123456789USA9001151M3001149<<<<<<<<<<<<<<<6]

[ Save traveller ]
```

---

## Error handling quick reference

| HTTP | `success` | Typical cause | Frontend action |
|------|-----------|---------------|-----------------|
| 200 | true | OK | Use `data` |
| 400 | false | Validation (file type, size, bad URL) | Show `message` to user |
| 401 | false | Missing / expired JWT | Redirect to login |
| 403 | false | Insufficient permissions | Show access-denied message |
| 413 | — | File exceeds server multipart limit (20 MB) | Show size error |
| 500 | false | R2 or DeepSeek API failure | Show generic error + retry button |

---

## Related docs

- [Booking Refund Admin API](./booking-refund-admin-api.md)
- [Ticket Action Admin API](./ticket-action-admin-api.md)
