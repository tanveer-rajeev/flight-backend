# Passport Extraction API

Upload a passport scan, store it in Cloudflare R2, and extract traveller fields using **Tesseract OCR** (full-page + MRZ region).

**Base path:** `/api/passport`  
**Controller:** `PassportExtractionController`  
**Service:** `PassportExtractionService`

---

## Overview

| Item | Detail |
|------|--------|
| Endpoint | `POST /api/passport/extract` |
| Auth | JWT Bearer token (required) |
| Content type | `multipart/form-data` |
| Accepted files | JPEG / PNG only |
| Max file size | 10 MB (app validation); 20 MB (Spring multipart hard limit) |
| Storage | R2 bucket under `passports/<uuid>.<ext>` |
| OCR engine | Tesseract (local) |

The image is **always uploaded to R2**, even when field extraction fails. Use `data.imageUrl` to display or persist the stored scan.

---

## Prerequisites

### Tesseract OCR

Tesseract must be installed on the server. The **tessdata path is auto-detected** by OS:

| OS | Auto-detected path |
|----|--------------------|
| Linux (CentOS/RHEL) | `/usr/share/tesseract/tessdata` |
| Linux (alt layout) | `/usr/share/tessdata` |
| Windows | `C:/Program Files/Tesseract-OCR/tessdata` |
| macOS (Homebrew) | `/opt/homebrew/share/tessdata` or `/usr/local/share/tessdata` |

**Linux servers also need native libs** (`leptonica`, `tesseract` packages — not just tessdata):

```bash
sudo dnf install -y leptonica tesseract tesseract-langpack-eng
sudo ldconfig
```

Override only for non-standard installs:

```properties
tesseract.datapath=/custom/path/to/tessdata
passport.ocr-max-dimension=2200
```

**CentOS / Rocky / AlmaLinux install guide:** [tesseract-centos-setup.md](./tesseract-centos-setup.md)

If Tesseract is missing or misconfigured, the API returns **502** with message `Passport OCR failed. Ensure Tesseract is installed correctly.`

### Cloudflare R2

R2 credentials must be configured for image upload:

```properties
r2.account-id=...
r2.access-key-id=...
r2.secret-access-key=...
r2.bucket-name=...
r2.public-url=https://pub-xxx.r2.dev
```

---

## Authentication

Every request requires a valid JWT:

```http
Authorization: Bearer <access_token>
```

Obtain a token via `POST /api/auth/login`. Without a token the server returns **401 Unauthorized**.

---

## Request

### `POST /api/passport/extract`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | Passport scan or clear photo (JPEG / PNG) |

### cURL (Linux / macOS / Git Bash)

```bash
# 1. Login
curl -s -X POST "http://localhost:8091/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"your-password"}'

# 2. Extract passport
curl -X POST "http://localhost:8091/api/passport/extract" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/passport.jpg"
```

### cURL (Windows PowerShell)

```powershell
curl.exe -X POST "http://localhost:8091/api/auth/login" `
  -H "Content-Type: application/json" `
  -d "{\"email\":\"user@example.com\",\"password\":\"your-password\"}"

curl.exe -X POST "http://localhost:8091/api/passport/extract" `
  -H "Authorization: Bearer YOUR_JWT_TOKEN" `
  -F "file=@C:\path\to\passport.jpg"
```

### JavaScript (fetch)

```js
const form = new FormData();
form.append('file', passportImageFile);

const res = await fetch('/api/passport/extract', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` },
  body: form,
});
const { success, message, data } = await res.json();
```

---

## Response envelope

All responses use `BaseResponse<T>`:

```json
{
  "success": true,
  "status": 200,
  "message": "...",
  "data": { ... }
}
```

Errors set `"success": false` and include an `error` object (or string for 401).

---

## Success responses

### 200 — Fields extracted

When OCR parses enough passport data (`extracted: true`):

```json
{
  "success": true,
  "status": 200,
  "message": "Passport data extracted successfully",
  "data": {
    "imageUrl": "https://pub-xxx.r2.dev/passports/550e8400-e29b-41d4-a716-446655440000.jpg",
    "surname": "RAHMAN",
    "givenNames": "ABDUL KARIM",
    "passportNumber": "A12345678",
    "nationality": "BGD",
    "dateOfBirth": "15 JAN 1990",
    "dateOfBirthIso": "1990-01-15",
    "dateOfExpiry": "14 JAN 2030",
    "dateOfExpiryIso": "2030-01-14",
    "dateOfIssue": "15 JAN 2020",
    "dateOfIssueIso": "2020-01-15",
    "gender": "M",
    "placeOfBirth": "DHAKA",
    "issuingCountry": "Bangladesh",
    "mrzLine1": "P<BGDRAHMAN<<ABDUL<KARIM<<<<<<<<<<<<<<<<<<",
    "mrzLine2": "A123456789BGD9001151M3001149<<<<<<<<<<<<<<<6",
    "extracted": true
  }
}
```

### 200 — Image saved, extraction incomplete

When the image uploads but OCR cannot parse structured fields (`extracted: false`):

```json
{
  "success": true,
  "status": 200,
  "message": "Image uploaded but extraction returned unstructured data — see rawExtraction",
  "data": {
    "imageUrl": "https://pub-xxx.r2.dev/passports/uuid.jpg",
    "extracted": false,
    "rawExtraction": "PASSPORT\nBANGLADESH\n... (raw Tesseract OCR text) ..."
  }
}
```

This is **not an HTTP error** — the upload succeeded. Show `rawExtraction` for manual review or prompt the user to retake the photo.

---

## Response field reference

| Field | Type | Description |
|-------|------|-------------|
| `imageUrl` | string | Permanent R2 URL of the stored passport image |
| `surname` | string \| null | Family name (uppercase) |
| `givenNames` | string \| null | Given / first + middle names |
| `passportNumber` | string \| null | Document number (Bangladesh format: `A12345678`) |
| `nationality` | string \| null | Nationality code or label |
| `dateOfBirth` | string \| null | Raw text from passport (e.g. `15 JAN 1990`) |
| `dateOfBirthIso` | string \| null | ISO date (`yyyy-MM-dd`) parsed from `dateOfBirth` |
| `dateOfExpiry` | string \| null | Expiry date raw text |
| `dateOfExpiryIso` | string \| null | ISO expiry date |
| `dateOfIssue` | string \| null | Issue date raw text |
| `dateOfIssueIso` | string \| null | ISO issue date |
| `gender` | string \| null | `M`, `F`, or `X` |
| `placeOfBirth` | string \| null | City / country of birth |
| `issuingCountry` | string \| null | Issuing country name |
| `mrzLine1` | string \| null | MRZ top line (44 chars when found) |
| `mrzLine2` | string \| null | MRZ bottom line (44 chars when found) |
| `extracted` | boolean | `true` = structured fields parsed; `false` = see `rawExtraction` |
| `rawExtraction` | string \| null | Raw OCR text — only when `extracted` is `false` |

---

## Error responses

### Quick reference

| HTTP | Condition | `message` | `error.code` |
|------|-----------|-----------|--------------|
| 401 | Missing / invalid JWT | `Unauthorized` | _(string, not ApiError)_ |
| 400 | Empty file | `Passport image file cannot be empty` | `BUSINESS_ERROR` |
| 400 | Not JPEG/PNG | `Passport image must be JPEG or PNG. Got: ...` | `BUSINESS_ERROR` |
| 400 | File > 10 MB | `Passport image too large. Maximum size is 10 MB. Got: ... bytes` | `BUSINESS_ERROR` |
| 413 | File > 20 MB | `File size exceeds the maximum allowed limit.` | `FILE_TOO_LARGE` |
| 502 | Corrupt / undecodable image | `Could not decode passport image.` | `MICROSERVICE_ERROR` |
| 502 | Tesseract failure | `Passport OCR failed. Ensure Tesseract is installed correctly.` | `MICROSERVICE_ERROR` |
| 400 | R2 upload failure | `Failed to upload file to R2` | `INVALID_FILE` |

Branch on `error.code` for programmatic handling. Use `error.traceId` to correlate with server logs.

---

### 401 — Unauthorized

```bash
curl -X POST "http://localhost:8091/api/passport/extract" \
  -F "file=@/path/to/passport.jpg"
```

```json
{
  "success": false,
  "status": 401,
  "message": "Unauthorized",
  "error": "Full authentication is required to access this resource"
}
```

---

### 400 — Empty file

```json
{
  "success": false,
  "status": 400,
  "message": "Passport image file cannot be empty",
  "error": {
    "code": "BUSINESS_ERROR",
    "traceId": "abc123..."
  }
}
```

---

### 400 — Invalid file type

```json
{
  "success": false,
  "status": 400,
  "message": "Passport image must be JPEG or PNG. Got: application/pdf",
  "error": {
    "code": "BUSINESS_ERROR",
    "traceId": "abc123..."
  }
}
```

---

### 400 — File too large (> 10 MB)

```json
{
  "success": false,
  "status": 400,
  "message": "Passport image too large. Maximum size is 10 MB. Got: 12582912 bytes",
  "error": {
    "code": "BUSINESS_ERROR",
    "traceId": "abc123..."
  }
}
```

---

### 413 — Spring multipart limit (> 20 MB)

```json
{
  "success": false,
  "status": 413,
  "message": "File size exceeds the maximum allowed limit.",
  "error": {
    "code": "FILE_TOO_LARGE",
    "traceId": "abc123..."
  }
}
```

---

### 502 — OCR / decode failure

```json
{
  "success": false,
  "status": 502,
  "message": "Passport OCR failed. Ensure Tesseract is installed correctly.",
  "error": {
    "code": "MICROSERVICE_ERROR",
    "traceId": "abc123..."
  }
}
```

---

### 400 — R2 upload failure

```json
{
  "success": false,
  "status": 400,
  "message": "Failed to upload file to R2",
  "error": {
    "code": "INVALID_FILE",
    "traceId": "abc123..."
  }
}
```

---

## Frontend integration notes

1. Use `<input type="file" accept="image/jpeg,image/png">`.
2. Validate file size client-side before upload (10 MB limit).
3. Always send the multipart field name **`file`** — omitting it causes an unhandled server error.
4. On success, persist `data.imageUrl` — this is the canonical reference to the stored scan.
5. When `data.extracted === false`, show a retry prompt; optionally display `data.rawExtraction` for support/debug.
6. Prefer ISO date fields (`dateOfBirthIso`, etc.) for form pre-fill; keep raw text fields for display.
7. Show a loading state during upload — OCR on large images can take several seconds.

### Suggested form pre-fill mapping

| API field | Traveller / booking field |
|-----------|---------------------------|
| `surname` + `givenNames` | `firstName`, `lastName` (split as needed) |
| `passportNumber` | `passportNo` |
| `dateOfBirthIso` | `dob` |
| `dateOfIssueIso` | `passportIssueDate` |
| `dateOfExpiryIso` | `passportExpiryDate` |
| `gender` | `gender` |
| `nationality` | `nationality` / `countryName` |
| `imageUrl` | `passportImageUrl` |

---

## How extraction works

1. Validate file type and size.
2. Upload raw image to R2 (`passports/` prefix).
3. Run Tesseract OCR on the full page (PSM 6).
4. Crop the bottom ~22% of the image and run MRZ-focused OCR (PSM 7, character whitelist).
5. Parse visual labels and MRZ lines; merge results with MRZ taking priority for document number and dates.
6. Set `extracted: true` when surname, date of birth, and (passport number or given names) are found.

Optimized for **Bangladesh passports** but handles generic MRZ formats when present.

---

## Related docs

- [R2 File Upload API](./r2-file-upload-passport-extraction-api.md) — generic R2 upload/delete endpoints at `/api/files`
