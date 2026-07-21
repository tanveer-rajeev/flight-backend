# Booking Pricing & Admin List API

Admin-facing reference for booking price fields (`originalPrice`, `buyPrice`, `profitLoss`), booking list endpoints by source type, create/import flows, and the sales report.

All booking price columns (`original_price`, `buy_price`, `booking_price`, `markup_amount`, `profit_loss`) are stored in **USD** at create time.

---

## Price model

| Field | DB column | Meaning |
|-------|-----------|---------|
| `originalPrice` | `original_price` | GDS / published fare before agency markup (USD) |
| `buyPrice` | `buy_price` | Supplier cost after commission provision (USD) |
| `bookingPrice` | `booking_price` | Sell price charged to the agency (USD) |
| `markupAmount` | `markup_amount` | Legacy markup tracking (`bookingPrice - originalPrice` at create) |
| `profitLoss` | `profit_loss` | **Profit/loss (USD)** = `bookingPrice - buyPrice` |

### Buy price (online bookings)

For online search → book flow, buy price is calculated in markup using `commissionProvision` on the matched markup rule:

```
buyPrice = (baseFare - commissionProvision) + tax
```

Sell-side customer discount still uses `commissionLessApplied` (unchanged).

Buy price is cached in markup data during search/validation and persisted on the booking at create.

### Fallback rules

| Scenario | `buyPrice` when omitted |
|----------|-------------------------|
| Manual booking | `originalPrice` |
| Import PNR | `originalPrice` |
| Legacy rows (no `buy_price`) | `originalPrice` |
| `profitLoss` when column empty | computed as `bookingPrice - buyPrice` |

### Migrations

- `V35__booking_buy_price.sql` — adds `buy_price`
- `V36__booking_profit_loss.sql` — adds `profit_loss` and backfills existing rows

---

## Authentication & permissions

| Action | Permission / role |
|--------|-------------------|
| View booking lists | `view-booking` |
| Create online booking | `create-booking` |
| Manual / import / group create | `create-booking-manual` |
| Sales report | `admin` role **or** `view-sales-report` |

Admin users (`provider = admin`) can filter lists by `currency` (agency wallet currency). Non-admin users only see their own (or parent agency) bookings.

---

## Booking list endpoints

Base path: `/api/bookings`

All list endpoints share the same query parameters and return `Page<BookingResponse>`.

### Query parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `currency` | string | No | Admin only. Filter by booker currency, e.g. `USD`, `BDT` |
| `fromDate` | date (`yyyy-MM-dd`) | No | Filter by `bookingDate` (inclusive start) |
| `toDate` | date (`yyyy-MM-dd`) | No | Filter by `bookingDate` (inclusive end) |
| `pnrOrId` | string | No | Match PNR or numeric booking ID |
| `ticketNo` | string | No | Match ticket number |
| `status` | string | No | Booking status enum, e.g. `CONFIRMED`, `TICKETED` |
| `page` | int | No | Zero-based page (default `0`) |
| `size` | int | No | Page size (default `10`) |

### Endpoints by source type

| Method | Path | `sourceType` filter | Description |
|--------|------|---------------------|-------------|
| `GET` | `/api/bookings/list` | `ONLINE` (+ null legacy) | Online GDS bookings |
| `GET` | `/api/bookings/list/manual` | `MANUAL` | Manual offline bookings |
| `GET` | `/api/bookings/list/import` | `IMPORT` | Imported PNR bookings |
| `GET` | `/api/bookings/list/group` | `GROUP` | Group ticket bookings |
| `GET` | `/api/bookings/list/all` | *(none)* | All source types |
| `GET` | `/api/bookings/list/user?userId=` | *(none)* | Bookings for a specific user |
| `GET` | `/api/bookings/{id}` | — | Single booking detail |

### Example — list all bookings

```http
GET /api/bookings/list/all?page=0&size=20&status=CONFIRMED
Authorization: Bearer <token>
```

### Example response (booking row)

```json
{
  "success": true,
  "message": "All bookings fetched successfully",
  "data": {
    "content": [
      {
        "id": 1205,
        "pnr": "ABC123",
        "sourceType": "ONLINE",
        "status": "CONFIRMED",
        "bookingPrice": "450.00",
        "originalPrice": "400.00",
        "buyPrice": "380.00",
        "markupAmount": "50.00",
        "profitLoss": "70.00",
        "exchangeCurrency": "BDT",
        "exchangeCurrencyRate": "110.5"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

**Note:** `bookingPrice`, `originalPrice`, `buyPrice`, and `profitLoss` in list/detail responses are the **stored USD values**. `exchangeCurrency` / `exchangeCurrencyRate` describe the agency wallet currency used for wallet deduction.

---

## Create endpoints (admin)

### Manual booking

```http
POST /api/bookings/create-manual
Permission: create-booking-manual
```

Key price fields in `ManualBookingRequest`:

| Field | Required | Description |
|-------|----------|-------------|
| `bookingPrice` | Yes | Sell price (agency currency input; stored as USD) |
| `originalPrice` | No | GDS / published price; defaults to `bookingPrice` |
| `buyPrice` | No | Supplier cost; defaults to `originalPrice` |
| `supplierId` | No | Used for auto supplier invoice |

Stored on booking (USD):

```
profitLoss = bookingPriceUsd - buyPriceUsd
```

### Import PNR

```http
POST /api/bookings/import-pnr
Permission: create-booking-manual
```

Key price fields in `ImportPnrRequest`:

| Field | Required | Description |
|-------|----------|-------------|
| `bookingPrice` | Yes | Sell price |
| `originalPrice` | No | GDS published price |
| `buyPrice` | No | Supplier cost; **defaults to `originalPrice`** when omitted |
| `supplierId` | No | Supplier for payable invoice |

Import sets `sourceType = IMPORT` (or `GROUP` for group provider) and `importedPnr = true`.

### Group ticket manual booking

```http
POST /api/bookings/group-ticket-manual
Permission: create-booking-manual
```

Creates a booking from an existing group ticket. `buyPrice` / `originalPrice` come from group ticket **costing**; `bookingPrice` from the fare total. `sourceType = GROUP`.

---

## Sales report (admin)

```http
GET /api/admin/report/sales
Permission: admin OR view-sales-report
```

### Query parameters

| Param | Type | Description |
|-------|------|-------------|
| `userId` | long | Filter by booking creator user |
| `agencyId` | long | Filter by agency (`business.id`) |
| `airlineCode` | string | Filter by travel segment airline code |
| `currency` | string | Filter by booker wallet currency |
| `from` | date | Created-at range start (defaults to today if both dates omitted) |
| `to` | date | Created-at range end |
| `page` | int | Page for confirmed ticket list (default `0`) |
| `size` | int | Page size (default `20`) |

### Included booking statuses

`CONFIRMED`, `TICKETED`, `TICKET_ISSUED`, `COMPLETED`

### Profit / loss calculation

Report totals and per-ticket rows use:

```
profitLoss = sellPrice - buyPrice
```

- Uses stored `profit_loss` (USD) when present
- Otherwise computed from `bookingPrice - buyPrice` (with `buyPrice` fallback to `originalPrice`)
- Amounts in `confirmedTickets` are converted to the **booker's wallet currency** using the booking exchange rate

### Example

```http
GET /api/admin/report/sales?from=2026-07-01&to=2026-07-31&agencyId=12&page=0&size=20
```

```json
{
  "success": true,
  "message": "Sales report retrieved successfully",
  "data": {
    "totalTickets": 42,
    "totalRevenue": 3150.75,
    "totalTax": 820.0,
    "netRevenue": 3150.75,
    "revenueAvg": 75.02,
    "salesTrend": [
      { "date": "2026-07-02", "totalTickets": 5, "revenue": 380.5 }
    ],
    "confirmedTickets": {
      "content": [
        {
          "bookingId": 1205,
          "pnr": "ABC123",
          "originalPrice": 44200.0,
          "buyPrice": 41990.0,
          "bookingPrice": 49725.0,
          "profitLoss": 7735.0,
          "markupAmount": 7735.0,
          "currency": "BDT",
          "status": "TICKETED"
        }
      ],
      "totalElements": 42
    }
  }
}
```

| Response field | Meaning |
|----------------|---------|
| `totalRevenue` | Sum of profit/loss (`sell - buy`) across matched bookings |
| `revenueAvg` | `totalRevenue / totalTickets` |
| `salesTrend[].revenue` | Daily sum of profit/loss |
| `confirmedTickets[].profitLoss` | Per-ticket profit/loss in booker currency |
| `confirmedTickets[].markupAmount` | Same as `profitLoss` (backward compatibility) |

---

## Markup rule fields (reference)

Relevant fields on `MarkupRule` (`/api/markup`) for pricing:

| Field | Used for |
|-------|----------|
| `commissionProvision` | **Buy price** — deducted from base fare before tax |
| `commissionLessApplied` | **Sell discount** — deducted from base fare for customer offer fare |
| `commissionType` | `PERCENTAGE` or `FIXED` (applies to both commission fields) |
| `markupValue` / `markupType` | Agency markup on offer fare |
| Plan `aitValue` / `aitType` | AIT applied on offer fare |

---

## Admin implementation checklist

1. Run migrations `V35` and `V36` before deploy.
2. Use `/api/bookings/list/all` for a unified admin booking table with `sourceType` column.
3. Display **profit/loss** from `profitLoss` (USD) on list/detail; optionally convert with `exchangeCurrencyRate` for display in agency currency.
4. On manual/import forms, collect optional `buyPrice`; leave blank to default to `originalPrice`.
5. Sales report dashboard should bind totals to `totalRevenue` and rows to `profitLoss` (not `bookingPrice - originalPrice`).
6. Online bookings automatically populate `buyPrice` from markup cache — no extra admin input required.

---

## Related endpoints

| Path | Purpose |
|------|---------|
| `GET /api/admin/report/refund` | Refund report |
| `POST /api/bookings/{id}/admin/refund` | Admin full/partial refund |
| `GET /api/markup/plans` | Markup plan CRUD |
| `GET /api/markup/plans/{id}/rules` | Rules including `commissionProvision` |
