# Group Ticket Admin API — Legs & Segments

Frontend implementation guide for creating and displaying group tickets with **connecting flights**, **round-trip**, and **multi-city** itineraries.

Admin base path: `/api/admin/group-tickets`  
Public listing (agency): `/api/flight/common/*`

**All read endpoints** return the same `GroupTicketDTO` shape, including **`flightInfos`** (flat) and **`legs`** (grouped).  
**Create/update with `legs`** is admin-only (`POST` / `PUT`).

---

## Concepts

| Term | Meaning | Example |
|------|---------|---------|
| **Trip type** (`flightType`) | Overall ticket shape | `One-Way`, `Round-Trip`, `Multi-City` |
| **Leg** | One origin → final destination journey | DAC → LHR (may include a stop) |
| **Segment** | One physical flight within a leg | DAC → DOH, DOH → LHR |

**Rules**

- Connecting flights within the same direction share the same **`leg`** number.
- **`segmentType`**: `ONEWAY` for outbound / multi-city legs; `RETURN` for the return leg on round-trip.
- Segments inside a leg must **chain**: segment N destination = segment N+1 origin.
- List order in `flightInfos` = travel order (used as booking `segmentOrder`).

---

## Endpoints

### Admin (create, update, list, detail)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/admin/group-tickets` | Create group ticket (`legs` or `flightInfos`) |
| `PUT` | `/api/admin/group-tickets/{gfCode}` | Update group ticket |
| `GET` | `/api/admin/group-tickets/{gfCode}` | Get one ticket |
| `GET` | `/api/admin/group-tickets` | List (paginated) |

Works for any `ticketType`: `GROUP`, `UMRAH`, or `A2A`.

### Public / agency (read-only — same response shape)

| Method | Path | Filter | Description |
|--------|------|--------|-------------|
| `GET` | `/api/flight/common/group-tickets` | Search params | Tickets matching route + date |
| `GET` | `/api/flight/common/group-special-fares` | `ticketType = GROUP` | Active online GROUP fares |
| `GET` | `/api/flight/common/group-umrah-fares` | `ticketType = UMRAH` | Active online UMRAH fares |
| `GET` | `/api/flight/common/group-a2a-fares` | `ticketType = A2A` | Active online A2A fares |

**Search params** for `/group-tickets`:

| Param | Required | Notes |
|-------|----------|-------|
| `departureDate` | Yes | `YYYY-MM-DD` |
| `origin` | No | IATA code |
| `destination` | No | IATA code |
| `returnDate` | No | Required for round-trip search to match `Round-Trip` tickets |

Umrah, A2A, and special-fare endpoints do **not** accept leg input — they only **return** stored `legs` / `flightInfos` from tickets created via admin. Use the same UI rendering for itinerary on all of them.

---

## Input: two supported formats

Send **either** grouped `legs` **or** flat `flightInfos`. If both are sent, **`legs` wins** and replaces `flightInfos`.

### Recommended: grouped `legs` (admin UI forms)

Best when the UI adds legs first, then connecting segments per leg.

### Alternative: flat `flightInfos`

Best when importing from GDS or copying a flat list. Each item must include `leg` and `segmentType`.

---

## Field reference

### Ticket-level (summary for search/list)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `gfCode` | string | Yes | Unique group fare code |
| `title` | string | Yes | Display title |
| `type` | string | Yes | Fare category e.g. `Non-Refundable` |
| `ticketType` | string | Yes | `GROUP`, `UMRAH`, or `A2A` |
| `status` | string | Yes | e.g. `Active`, `Expired` |
| `airlineCode` | string | Yes | e.g. `QR` |
| `airlineName` | string | Yes | e.g. `Qatar Airways` |
| `vendorName` | string | Yes | |
| `bookingStarts` | date | Yes | `YYYY-MM-DD` |
| `bookingEnds` | date | Yes | `YYYY-MM-DD` |
| `origin` | string | Yes | Overall origin (first leg start) |
| `destination` | string | Yes | Overall destination (outbound end for RT; last stop for MC) |
| `fareCurrency` | string | Yes | e.g. `USD` |
| `gdsPnr` | string | No | |
| `airlinePnr` | string | No | |
| `departureDate` | date | Yes | First departure date |
| `departureTime` | string | Yes | First departure time `HH:mm` |
| `arrivalDate` | date | Yes | Final arrival date |
| `arrivalTime` | string | Yes | Final arrival time `HH:mm` |
| `flightType` | string | Yes | `One-Way`, `Round-Trip`, or `Multi-City` |
| `supplierId` | long | No | Linked supplier |
| `costing` | number | No | Buy price per pax |
| `saleStatus` | string | No | `ONLINE` or `OFFLINE` |
| `passengerFares` | array | No | Fare buckets |
| `legs` | array | No | **Input:** grouped legs. **Response:** always set when segments exist |
| `flightInfos` | array | No | Flat segment list |

### Leg object (`legs[]`)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `leg` | integer | No | 1-based; auto-increments if omitted |
| `segmentType` | string | No | `ONEWAY` (default) or `RETURN` |
| `origin` | string | Response only | First segment origin |
| `destination` | string | Response only | Last segment destination |
| `segments` | array | Yes (input) | Connecting flights in this leg |

### Segment object (`segments[]` / `flightInfos[]`)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `origin` | string | Yes | IATA airport code |
| `destination` | string | Yes | IATA airport code |
| `departureDate` | date | Yes | |
| `departureTime` | string | No | `HH:mm` |
| `arrivalDate` | string | No | `YYYY-MM-DD` |
| `arrivalTime` | string | No | `HH:mm` |
| `flightNumber` | string | No | e.g. `QR601` |
| `durationInMinutes` | integer | No | |
| `stops` | integer | No | Stops on this segment |
| `equipment` | string | No | Aircraft type |
| `originTerminal` | string | No | |
| `destinationTerminal` | string | No | |
| `leg` | integer | No | Set automatically when using `legs[]`; required per item for flat `flightInfos` |
| `segmentType` | string | No | `ONEWAY` or `RETURN`; inherited from leg when using `legs[]` |

---

## Examples

### 1. One-way with connecting flights (1 leg, 2 segments)

**Route:** DAC → KUL → DXB

```json
{
  "gfCode": "GF-DAC-DXB-001",
  "title": "DAC to DXB via KUL",
  "type": "Non-Refundable",
  "ticketType": "GROUP",
  "status": "Active",
  "airlineCode": "AK",
  "airlineName": "AirAsia",
  "vendorName": "Vendor A",
  "bookingStarts": "2026-07-01",
  "bookingEnds": "2026-08-31",
  "origin": "DAC",
  "destination": "DXB",
  "fareCurrency": "USD",
  "departureDate": "2026-08-01",
  "departureTime": "08:00",
  "arrivalDate": "2026-08-01",
  "arrivalTime": "20:30",
  "flightType": "One-Way",
  "saleStatus": "ONLINE",
  "legs": [
    {
      "leg": 1,
      "segmentType": "ONEWAY",
      "segments": [
        {
          "origin": "DAC",
          "destination": "KUL",
          "departureDate": "2026-08-01",
          "departureTime": "08:00",
          "arrivalDate": "2026-08-01",
          "arrivalTime": "12:00",
          "flightNumber": "AK123",
          "durationInMinutes": 240
        },
        {
          "origin": "KUL",
          "destination": "DXB",
          "departureDate": "2026-08-01",
          "departureTime": "14:00",
          "arrivalDate": "2026-08-01",
          "arrivalTime": "20:30",
          "flightNumber": "AK456",
          "durationInMinutes": 450
        }
      ]
    }
  ],
  "passengerFares": [
    {
      "fareBasis": "adult",
      "quantity": 20,
      "currency": "USD",
      "baseFare": 450.0,
      "equivalentBaseFare": 450.0,
      "equivalentTaxes": 80.0
    }
  ]
}
```

### 2. Round-trip with connecting both ways (2 legs, 4 segments)

**Outbound:** DAC → DOH → LHR  
**Return:** LHR → DOH → DAC

```json
{
  "gfCode": "GF-DAC-LHR-RT-001",
  "title": "DAC-LHR Round Trip",
  "type": "Non-Refundable",
  "ticketType": "GROUP",
  "status": "Active",
  "airlineCode": "QR",
  "airlineName": "Qatar Airways",
  "vendorName": "Vendor A",
  "bookingStarts": "2026-07-01",
  "bookingEnds": "2026-12-31",
  "origin": "DAC",
  "destination": "LHR",
  "fareCurrency": "USD",
  "departureDate": "2026-08-01",
  "departureTime": "03:30",
  "arrivalDate": "2026-08-15",
  "arrivalTime": "06:00",
  "flightType": "Round-Trip",
  "saleStatus": "ONLINE",
  "legs": [
    {
      "leg": 1,
      "segmentType": "ONEWAY",
      "segments": [
        {
          "origin": "DAC",
          "destination": "DOH",
          "departureDate": "2026-08-01",
          "departureTime": "03:30",
          "flightNumber": "QR601"
        },
        {
          "origin": "DOH",
          "destination": "LHR",
          "departureDate": "2026-08-01",
          "departureTime": "08:00",
          "flightNumber": "QR3"
        }
      ]
    },
    {
      "leg": 2,
      "segmentType": "RETURN",
      "segments": [
        {
          "origin": "LHR",
          "destination": "DOH",
          "departureDate": "2026-08-15",
          "departureTime": "16:00",
          "flightNumber": "QR4"
        },
        {
          "origin": "DOH",
          "destination": "DAC",
          "departureDate": "2026-08-15",
          "departureTime": "23:00",
          "flightNumber": "QR602"
        }
      ]
    }
  ]
}
```

**Public search:** pass `returnDate` when querying round-trip tickets:

```
GET /api/flight/common/group-tickets?departureDate=2026-08-01&origin=DAC&destination=LHR&returnDate=2026-08-15
```

### 3. Multi-city (3 legs, one with connection)

**Route:** DAC → DXB, DXB → IST, IST → DOH → DAC

```json
{
  "flightType": "Multi-City",
  "origin": "DAC",
  "destination": "DAC",
  "departureDate": "2026-08-01",
  "arrivalDate": "2026-08-20",
  "legs": [
    {
      "leg": 1,
      "segmentType": "ONEWAY",
      "segments": [
        { "origin": "DAC", "destination": "DXB", "departureDate": "2026-08-01", "flightNumber": "EK583" }
      ]
    },
    {
      "leg": 2,
      "segmentType": "ONEWAY",
      "segments": [
        { "origin": "DXB", "destination": "IST", "departureDate": "2026-08-05", "flightNumber": "TK759" }
      ]
    },
    {
      "leg": 3,
      "segmentType": "ONEWAY",
      "segments": [
        { "origin": "IST", "destination": "DOH", "departureDate": "2026-08-15", "flightNumber": "QR242" },
        { "origin": "DOH", "destination": "DAC", "departureDate": "2026-08-15", "flightNumber": "QR639" }
      ]
    }
  ]
}
```

### 4. Flat `flightInfos` input (same one-way connecting example)

```json
{
  "flightType": "One-Way",
  "flightInfos": [
    {
      "leg": 1,
      "segmentType": "ONEWAY",
      "origin": "DAC",
      "destination": "KUL",
      "departureDate": "2026-08-01",
      "flightNumber": "AK123"
    },
    {
      "leg": 1,
      "segmentType": "ONEWAY",
      "origin": "KUL",
      "destination": "DXB",
      "departureDate": "2026-08-01",
      "flightNumber": "AK456"
    }
  ]
}
```

---

## Response shape

Create, update, get, and list all return the same ticket object. When segments exist, both views are included:

```json
{
  "success": true,
  "data": {
    "gfCode": "GF-DAC-DXB-001",
    "flightType": "One-Way",
    "origin": "DAC",
    "destination": "DXB",
    "flightInfos": [
      {
        "leg": 1,
        "segmentType": "ONEWAY",
        "origin": "DAC",
        "destination": "KUL",
        "departureDate": "2026-08-01",
        "flightNumber": "AK123"
      },
      {
        "leg": 1,
        "segmentType": "ONEWAY",
        "origin": "KUL",
        "destination": "DXB",
        "departureDate": "2026-08-01",
        "flightNumber": "AK456"
      }
    ],
    "legs": [
      {
        "leg": 1,
        "segmentType": "ONEWAY",
        "origin": "DAC",
        "destination": "DXB",
        "segments": [
          { "leg": 1, "segmentType": "ONEWAY", "origin": "DAC", "destination": "KUL", "flightNumber": "AK123" },
          { "leg": 1, "segmentType": "ONEWAY", "origin": "KUL", "destination": "DXB", "flightNumber": "AK456" }
        ]
      }
    ]
  }
}
```

### Frontend display tips

| UI need | Use |
|---------|-----|
| Itinerary accordion by direction/city-pair | `legs[]` |
| Timeline of all flights | `flightInfos[]` in order |
| Outbound vs return badge | `segmentType` on leg or segment |
| Connecting indicator | `legs[n].segments.length > 1` |

---

## Validation errors

| Condition | Error |
|-----------|-------|
| Leg with empty `segments` | `Each leg must contain at least one segment` |
| Broken connection inside leg | `Connecting segments on leg X must chain...` |
| `Round-Trip` without any `RETURN` segment | `Round-trip group tickets must include at least one RETURN segment` |
| Missing origin/destination on segment | `Each flight segment must have origin and destination` |

---

## `flightType` values

| Value | Booking trip type |
|-------|-------------------|
| `One-Way` | `ONE_WAY` |
| `Round-Trip` | `ROUND_TRIP` |
| `Multi-City` | `MULTI_CITY` |

Hyphen/space variants are normalized internally (`Round Trip`, `ROUND_TRIP`, etc.).

---

## Update behaviour

- Send **`legs`** from the form (recommended) **or** **`flightInfos`** — not both unless you intend `legs` to replace the flat list.
- On update, providing `flightInfos` replaces all stored segments (same as before).
- Omitting both `legs` and `flightInfos` leaves existing segments unchanged.

---

## Booking integration

When a group ticket is booked manually, each `flightInfo` row becomes a booking segment with:

- `segmentOrder` = list index
- `segmentType` = stored `ONEWAY` / `RETURN`
- `tripType` = derived from ticket `flightType`

No extra payload is needed at booking time; segments are read from the stored group ticket.

---

## Legacy data (pre-legs migration)

Existing inventory is backfilled automatically on deploy:

| Column | Default for old rows |
|--------|----------------------|
| `leg` | `1` |
| `segment_type` | `ONEWAY` |

**Behaviour for old tickets**

- API responses always include `leg: 1` and `segmentType: "ONEWAY"` even if the DB had nulls (read-path defaults).
- Old round-trip tickets that never had `RETURN` segments are treated as **legacy** — they can still be listed, fetched, and updated without failing validation.
- New round-trip tickets created with the leg UI must include a `RETURN` leg (leg 2+).

Migrations: `V49__flight_info_leg_segment_type.sql`, `V50__flight_info_leg_segment_type_backfill.sql`
