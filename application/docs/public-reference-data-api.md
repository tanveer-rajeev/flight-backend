# Public Reference Data API — Frontend Guide

Unauthenticated endpoints for bootstrapping the customer-facing site: branding, airports, and airlines.

> **Base URL:** `{api.domain}` (e.g. `https://api.kingstartravel.com`)
>
> **Auth:** None required — all routes under `/api/public/**` are public.
>
> **Response envelope:** Every endpoint returns the standard wrapper:

```json
{
  "success": true,
  "message": "...",
  "status": 200,
  "data": { }
}
```

---

## Table of Contents

1. [Platform Info](#1-platform-info)
2. [Airports](#2-airports)
3. [Airlines](#3-airlines)
4. [Recommended Frontend Bootstrap Flow](#4-recommended-frontend-bootstrap-flow)

---

## 1. Platform Info

Branding, contact details, theme colors, and social links.

### GET `/api/public/platform-info`

**Upstream source:** `{platform.info.url}/api/platform-info/me` (server-side; `x-api-key` is not exposed to the client).

**Example request**

```http
GET /api/public/platform-info
Accept: application/json
```

**Example response**

```json
{
  "success": true,
  "message": "Platform information retrieved successfully",
  "status": 200,
  "data": {
    "keyValues": {
      "copyright": "© 2026 kingstartravel.com— All rights reserved.",
      "address": "Dubai , United Arab Emirates",
      "facebook": "https://www.facebook.com/profile.php?id=61559652371452",
      "supportPhone": "+8801825-766252",
      "bd_phone": "+8801958070808 -22",
      "instagram": "https://www.instagram.com/kingstartravel/",
      "linkedin": " ",
      "bgImage": "https://shorturl.at/hBGzI",
      "uae_phone": "+971527722842",
      "wp_message": "Welcome Star Travel.. :)",
      "supportAddress": "Dubai , United Arab Emirates",
      "twitter": "https://x.com/KingstarTravels",
      "supportEmail": "support@kingstartravel.com",
      "networkPaymentEnable": "0",
      "wp_number": "+8801825766252",
      "socialSectionTitle": "Follow Us",
      "email": "info@kingstartravel.com"
    },
    "platform": {
      "name": "KING STAR TRAVEL",
      "domain": "kingstartravel.com",
      "slug": "king-star-travel",
      "isActive": true,
      "platformDetails": {
        "privacyPolicy": "<p>KING STAR TRAVEL</p>",
        "termsAndConditions": "<p>KING STAR TRAVEL</p>",
        "theme": "classic",
        "logoUrl": "https://.../logos/platform_24_....webp",
        "faviconUrl": "https://.../favicons/platform_24_....png",
        "description": "<p>KING STAR TRAVEL</p>",
        "icon": "https://.../logos/platform_24_....png",
        "primaryColor": "#bb0c0c",
        "contactName": "HelpLine",
        "contactPhone": "0542040355",
        "contactEmail": "info@kingstartravel.com",
        "address": "nife"
      }
    },
    "markupPlans": []
  }
}
```

**Frontend usage**

| Field | Use |
|-------|-----|
| `data.platform.platformDetails.logoUrl` | Header / footer logo |
| `data.platform.platformDetails.faviconUrl` | Browser tab icon |
| `data.platform.platformDetails.primaryColor` | Theme accent color |
| `data.platform.platformDetails.theme` | Layout variant (`classic`, etc.) |
| `data.keyValues` | Footer contact, social links, WhatsApp, phones |
| `data.platform.platformDetails.privacyPolicy` / `termsAndConditions` | Legal pages (HTML) |

> **Note:** `providersMap`, `providers`, `platform.id`, `platform.platformDetails.id`, and `platform.apiKey` are intentionally omitted from the public response.

**Caching:** Loaded once at server startup. Restart the backend after platform config changes upstream.

---

## 2. Airports

Full airport list for autocomplete, search widgets, and location pickers.

### GET `/api/public/airports`

**Data source:** Local backend database (same records as `/api/admin/airport-airline/airport/list`).

**Example request**

```http
GET /api/public/airports
Accept: application/json
```

**Example response**

```json
{
  "success": true,
  "message": "Success",
  "status": 200,
  "data": [
    {
      "id": 719,
      "Code": "CPO",
      "Name": "Chamonate Arpt",
      "CityName": "Copiapo",
      "CityCode": "CPO",
      "CountryCode": "CL",
      "CountryName": "CHILE",
      "Lat": "-27",
      "Lon": "-70",
      "Timezone": "-4",
      "NumAirports": 1,
      "City": "XXX",
      "ActiveSuggestion": null
    }
  ]
}
```

**Airport object fields**

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Internal record ID |
| `Code` | string | IATA airport code (e.g. `DAC`, `DXB`) |
| `Name` | string | Airport name |
| `CityName` | string | City display name |
| `CityCode` | string | City IATA code |
| `CountryCode` | string | ISO country code |
| `CountryName` | string | Country name |
| `Lat` | string | Latitude |
| `Lon` | string | Longitude |
| `Timezone` | string | UTC offset |
| `NumAirports` | number | Airports in city cluster |
| `City` | string | City grouping code |
| `ActiveSuggestion` | number \| null | Suggestion flag |

**Frontend usage**

- Build airport autocomplete: search on `Code`, `CityName`, `Name`, `CountryName`.
- Display format suggestion: `{CityName} ({Code}) — {CountryName}`.
- Use `Code` as the value sent to flight search APIs.

> **Tip:** This list can be large. Cache it in memory/localStorage on first load and refresh periodically (e.g. daily).

**Admin search (optional, also public):** For single-result lookup by query, the existing admin route remains available without auth:

```http
GET /api/admin/airport-airline/airport-search?query=DAC
```

---

## 3. Airlines

Full airline list for logos, filters, and display names.

### GET `/api/public/airlines`

**Upstream source:** `{platform.info.url}/api/airline/list` (server-side proxy with `x-api-key`).

**Example request**

```http
GET /api/public/airlines
Accept: application/json
```

**Example response**

```json
{
  "success": true,
  "message": "Success",
  "status": 200,
  "data": [
    {
      "AirlineId": 1,
      "FS": "LCI",
      "IATA": "LF",
      "ICAO": "LCI",
      "Name": "Lao Central Airlines ",
      "Active": 1,
      "IsDomestic": null
    }
  ]
}
```

**Airline object fields**

| Field | Type | Description |
|-------|------|-------------|
| `AirlineId` | number | Internal airline ID |
| `FS` | string | FlightStats / internal code |
| `IATA` | string | IATA airline code (e.g. `EK`, `BG`) — primary key for UI |
| `ICAO` | string | ICAO code |
| `Name` | string | Airline display name |
| `Active` | number | `1` = active, `0` = inactive |
| `IsDomestic` | number \| null | Domestic flag when set |

**Frontend usage**

- Index airlines by `IATA` for O(1) lookup when rendering search results.
- Filter to `Active === 1` if you only want bookable carriers.
- Display format: `{Name} ({IATA})`.

**Caching:** Loaded once at server startup. Restart the backend after airline data changes upstream.

---

## 4. Recommended Frontend Bootstrap Flow

On app init (or layout mount), fetch reference data in parallel:

```typescript
const [platformInfo, airports, airlines] = await Promise.all([
  fetch(`${API_BASE}/api/public/platform-info`).then(r => r.json()),
  fetch(`${API_BASE}/api/public/airports`).then(r => r.json()),
  fetch(`${API_BASE}/api/public/airlines`).then(r => r.json()),
]);

// Apply theme
const { primaryColor, logoUrl, faviconUrl } =
  platformInfo.data.platform.platformDetails;
document.documentElement.style.setProperty('--primary-color', primaryColor);

// Build lookup maps
const airportByCode = Object.fromEntries(
  airports.data.map(a => [a.Code, a])
);
const airlineByIata = Object.fromEntries(
  airlines.data.filter(a => a.Active === 1).map(a => [a.IATA, a])
);
```

Store `airports` and `airlines` in global state (React Context, Zustand, Pinia, etc.) to avoid re-fetching on every page.

---

## Error Handling

| HTTP status | Meaning |
|-------------|---------|
| `200` | Success — check `success: true` |
| `401` | Not applicable for these routes |
| `500` | Server error — upstream platform data may be unavailable at startup |

If the backend fails to load platform info or airlines at startup, the application will not start. Airports come from the local DB and are always available once the DB is connected.

---

## Route Summary

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/public/platform-info` | None | Branding, theme, contacts |
| GET | `/api/public/airports` | None | Full airport list |
| GET | `/api/public/airlines` | None | Full airline list (platform proxy) |
