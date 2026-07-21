# Ticket Action Admin API

Admin-facing reference for void / cancel / refund ticket action requests, including **supplier refund cost** on finalize (after the user accepts the quote).

Base paths:

- Admin: `/api/admin/ticket-actions`
- Agency (booking-scoped): `/api/bookings/{bookingId}/ticket-actions`

Related: [Booking Refund Admin API](./booking-refund-admin-api.md) (direct admin refund without ticket action flow).

---

## Flow overview

```mermaid
sequenceDiagram
    participant Agency
    participant Admin
    participant System

    Agency->>System: POST ticket action (VOID/CANCEL/REFUND)
    Admin->>System: POST quote (customer fees)
    Agency->>System: POST confirm quote
    Admin->>System: POST start-processing (optional)
    Admin->>System: POST finalize + supplierRefundCost
    System->>Agency: Wallet credit (sell price - quote total)
    System->>System: Supplier payable reversal (buy - supplierRefundCost)
```

| Step | Status | Who |
|------|--------|-----|
| 1. Submit | `SUBMITTED` | Agency user |
| 2. Quote | `QUOTED` | Admin |
| 3. Confirm | `USER_CONFIRMED` | Agency user |
| 4. Processing (optional) | `ADMIN_PROCESSING` | Admin |
| 5. Finalize | `COMPLETED` or `FAILED` | Admin |

---

## Two independent amounts (framed by profitLoss)

**`profitLoss`** = `bookingPrice − buyPrice` (original margin) is the **key metric** for admin decisions.

| Side | When set | Field | Meaning |
|------|----------|-------|---------|
| **Margin (key)** | From booking | `profitLoss` / `netProfitLoss` | Original margin and outcome after finalize |
| **Customer** | Admin **quote** (before user confirms) | `totalAmount` (+ airline/service breakdown) | Fee charged to customer; wallet credit = sell price − quote total |
| **Supplier** | Admin **finalize** (after user confirms) | `supplierRefundCost` | Cost supplier keeps; remaining payable for PNR |

```
profitLoss = bookingPrice - buyPrice
netProfitLoss = profitLoss - supplierRefundCost + quoteTotalAmount
```

These amounts are **not linked**. Example:

| Item | USD |
|------|-----|
| Buy price | 900 |
| Sell price | 1,200 |
| **profitLoss** | **300** |
| Quote total (customer fee) | 400 → wallet credit **800** |
| `supplierRefundCost` on finalize | 200 → payable reversed **700**, remaining **200** |
| **netProfitLoss** | 300 − 200 + 400 = **500** |

---

## Permissions

| Action | Permission |
|--------|------------|
| List / view requests | `admin-view-ticket-action-requests` |
| Quote / reject | `admin-quote-ticket-action-request` |
| Start processing / finalize | `admin-finalize-ticket-action-request` |
| Agency create / confirm | `view-booking` + booking access |

---

## Admin endpoints

### GET `/api/admin/ticket-actions`

List all ticket action requests.

| Query | Description |
|-------|-------------|
| `status` | e.g. `USER_CONFIRMED`, `QUOTED`, `COMPLETED` |
| `type` | `VOID`, `CANCEL`, `REFUND` |
| `page`, `size` | Pagination |

### GET `/api/admin/ticket-actions/confirmed`

Shortcut for `status=USER_CONFIRMED` — queue ready for processing/finalize.

### POST `/api/admin/ticket-actions/{requestId}/quote`

Send quote to customer (customer-side fees only).

```json
{
  "airlineCost": 300.00,
  "serviceCharge": 100.00,
  "totalAmount": 400.00,
  "currency": "USD",
  "details": "Airline penalty + service fee",
  "adminNote": "Non-refundable fare",
  "acceptDeadline": "2026-07-10T23:59:59",
  "refundTimeline": "3-5 business days"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `airlineCost` | Yes | Airline portion of customer fee |
| `serviceCharge` | Yes | Agency service charge |
| `totalAmount` | Yes | Total customer fee (deducted from sell price on completion) |
| `acceptDeadline` | No | Auto-reject if user does not confirm in time |
| `refundTimeline` | No | Display-only timeline for customer |

### POST `/api/admin/ticket-actions/{requestId}/reject`

Reject request (any non-terminal status).

### POST `/api/admin/ticket-actions/{requestId}/start-processing`

Move `USER_CONFIRMED` → `ADMIN_PROCESSING` (optional).

### POST `/api/admin/ticket-actions/{requestId}/finalize`

Complete or fail the request. **Supplier cost is captured here.**

```json
{
  "resultStatus": "COMPLETED",
  "finalResult": "Void processed with airline",
  "externalReference": "AIR-REF-12345",
  "supplierRefundCost": 200.00
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `resultStatus` | Yes | `COMPLETED` or `FAILED` |
| `finalResult` | No | Notes shown to customer |
| `externalReference` | No | Airline/GDS reference |
| `supplierRefundCost` | **Yes when COMPLETED** | Supplier cost kept (USD). Use `0` for full supplier credit |

#### When `resultStatus = COMPLETED`

1. **Wallet** — credits agency: `bookingPrice − quoteTotalAmount` (customer fee from quote).
2. **Booking** — status updated via ticket action type (`VOID`, `TICKET_CANCELLED`, etc.).
3. **Supplier payable** — same rules as [booking refund](./booking-refund-admin-api.md):
   - `supplierPayableReversed = buyPrice − supplierRefundCost`
   - `remainingSupplierPayable = supplierRefundCost`
4. **Audit** — values stored on the ticket action request record.

#### When `resultStatus = FAILED`

No wallet refund, no supplier adjustment. `supplierRefundCost` is ignored.

#### Validation

| Rule | Error |
|------|-------|
| `supplierRefundCost` missing on COMPLETED | Required error |
| `supplierRefundCost` > `buyPrice` | Cannot exceed buy price |
| Finalize from wrong status | Must be `USER_CONFIRMED` or `ADMIN_PROCESSING` |

---

## Response fields (admin UI)

After finalize, `TicketActionRequestResponse` includes:

| Field | Description |
|-------|-------------|
| `buyPrice` | Booking supplier buy price (USD) |
| `profitLoss` | **Original margin** (`bookingPrice − buyPrice`) — key reference |
| `netProfitLoss` | **After finalize** = `profitLoss − supplierRefundCost + quoteTotalAmount` |
| `totalAmount` | Customer quote total (fee) |
| `supplierRefundCost` | Supplier cost kept (set on COMPLETED) |
| `supplierPayableReversed` | Removed from supplier payable |
| `remainingSupplierPayable` | Still owed to supplier for this PNR |
| `refunded` | `true` when wallet/supplier processing ran |

---

## Agency endpoints (reference)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/bookings/{bookingId}/ticket-actions` | Submit request |
| GET | `/api/bookings/{bookingId}/ticket-actions` | List for booking |
| GET | `/api/bookings/{bookingId}/ticket-actions/{requestId}` | Detail |
| POST | `/api/bookings/{bookingId}/ticket-actions/{requestId}/confirm` | Accept quote |

---

## Admin UI — finalize screen

Show after user confirmed (`USER_CONFIRMED` or `ADMIN_PROCESSING`):

```
Booking
  PNR: ABC123
  Sell price: 1,200 USD
  Buy price:  900 USD
  profitLoss: 300 USD   ← original margin (key)

Customer quote (already accepted)
  Airline cost:    300
  Service charge:  100
  Total fee:       400  → wallet credit: 800

Supplier adjustment
  Supplier cost kept: [200.00]  (required)
  → Payable reversed: 700
  → Remaining payable (PNR): 200
  → netProfitLoss: 500   ← profitLoss - supplierRefundCost + quoteTotal

Result
  ( ) COMPLETED   ( ) FAILED
  Final result: [________________]
  External ref: [________________]

[ Finalize ]
```

---

## Migration

- `V48__ticket_action_supplier_refund_cost.sql` — adds `supplier_refund_cost`, `supplier_payable_reversed`, `remaining_supplier_payable` to `ticket_action_request`.
