# Admin Booking Refund API

Admin-facing reference for issuing full or partial customer refunds and adjusting supplier payable using an explicit **supplier refund cost**.

Base path: `/api/bookings`

---

## Overview

A booking refund has **two independent amounts**, framed against the booking **`profitLoss`** (original margin = `bookingPrice − buyPrice`):

| Side | Field | Meaning |
|------|-------|---------|
| **Customer (agency wallet)** | `refundType` + `deductionAmount` | How much is credited back to the agency user’s wallet |
| **Supplier (payable)** | `supplierRefundCost` | How much the supplier keeps; remaining payable for the PNR |
| **Margin (key)** | `profitLoss` / `netProfitLoss` | Original margin and outcome after refund |

Customer refund and supplier cost are **not linked**. Use **`profitLoss`** to see what the agency earned on the booking, and **`netProfitLoss`** to see the margin after supplier cost and any customer fee kept.

```
profitLoss = bookingPrice - buyPrice
netProfitLoss = profitLoss - supplierRefundCost + customerFeeKept
```

For admin refund, `customerFeeKept` = `deductionAmount` (0 on FULL).  
All amounts in **USD** unless noted.

---

## Authentication & permission

| Action | Permission |
|--------|------------|
| Admin refund | `update-booking-admin` |

---

## Endpoint

### POST `/api/bookings/{id}/admin/refund`

Marks the booking `REFUND`, credits the agency wallet, and adjusts supplier payable.

#### Path parameters

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Booking ID |

#### Request body

```json
{
  "refundType": "FULL",
  "deductionAmount": null,
  "supplierRefundCost": 200.00,
  "reason": "Customer cancelled; supplier penalty 200"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refundType` | `"FULL"` \| `"PARTIAL"` | Yes | Customer refund mode |
| `deductionAmount` | decimal | PARTIAL only | Fee **kept from customer** (not refunded to wallet). Must be &gt; 0 and ≤ sell price |
| `supplierRefundCost` | decimal | Yes | Amount **supplier keeps** on refund (USD). Use `0` when supplier credits full buy price. Must be ≥ 0 and ≤ `buyPrice` |
| `reason` | string | Yes | Audit / timeline reason |

#### Customer refund rules

| `refundType` | Wallet credit |
|--------------|---------------|
| `FULL` | Full `bookingPrice` (sell price) |
| `PARTIAL` | `bookingPrice - deductionAmount` |

#### Supplier payable rules

Uses booking `buyPrice` (falls back to `originalPrice`, then `bookingPrice`).

```
supplierPayableReversed = buyPrice - supplierRefundCost
remainingSupplierPayable = supplierRefundCost   // against this PNR
```

**Example** (your scenario):

| Item | Amount (USD) |
|------|----------------|
| Buy price | 900 |
| Sell price | 1,200 |
| **profitLoss** | **300** |
| Customer refund | FULL → 1,200 to wallet |
| `supplierRefundCost` | 200 |
| Payable reversed | 900 − 200 = **700** |
| Remaining payable (PNR) | **200** |
| **netProfitLoss** | 300 − 200 + 0 = **100** |

Another example — partial customer refund, full supplier credit:

| Item | Value |
|------|-------|
| Sell price | 1,200 |
| `refundType` | `PARTIAL` |
| `deductionAmount` | 300 (customer receives 900) |
| `supplierRefundCost` | 0 |
| Payable reversed | full buy price |

---

## Response

```json
{
  "success": true,
  "message": "FULL refund processed successfully for booking 42",
  "data": {
    "bookingId": 42,
    "pnr": "ABC123",
    "ticketNo": "1234567890",
    "bookingPrice": 1200.00,
    "deductionAmount": 0,
    "refundedAmount": 1200.00,
    "buyPrice": 900.00,
    "profitLoss": 300.00,
    "netProfitLoss": 100.00,
    "supplierRefundCost": 200.00,
    "supplierPayableReversed": 700.00,
    "remainingSupplierPayable": 200.00,
    "refundType": "FULL",
    "reason": "Customer cancelled; supplier penalty 200",
    "currency": "BDT"
  }
}
```

| Field | Description |
|-------|-------------|
| `bookingPrice` | Original sell price (USD) |
| `deductionAmount` | Customer fee kept (0 for FULL) |
| `refundedAmount` | Credited to agency wallet (USD booking price basis) |
| `buyPrice` | Supplier buy price used for payable math |
| `profitLoss` | **Original margin** (`bookingPrice − buyPrice`) — key reference |
| `netProfitLoss` | **After refund** = `profitLoss − supplierRefundCost + deductionAmount` |
| `supplierRefundCost` | Supplier cost kept |
| `supplierPayableReversed` | Amount removed from supplier payable |
| `remainingSupplierPayable` | Net supplier cost still owed for this PNR |
| `currency` | Booker display currency |

---

## Side effects

All money and supplier work runs in **one DB transaction** (`@Transactional` on `adminRefundBooking`).  
If supplier reverse fails, wallet credit and booking status change **roll back** together.

1. **Booking status** → `REFUND` (rejected if already `REFUND`)
2. **Wallet** — append-only: new REFUND deposit + transaction; original PURCHASE kept
3. **Supplier payable** — reversal transaction (`payableAmount` negative) for `supplierPayableReversed`; supplier master payable reduced accordingly
4. **Invoice** — linked supplier invoice marked `REJECTED` when present
5. **Group tickets** — booked quantity restored on the group ticket PNR
6. **Notifications** — user notified of refund (best-effort; failure does not roll back)
7. **Audit** — `BOOKING_REFUNDED` activity log via `logAdminRefund` + booking timeline entry

Audit metadata includes: `channel=ADMIN_REFUND`, `appendOnly=true`, `refundType`, `deductionAmount`, `refundedAmount`, `buyPrice`, `supplierRefundCost`, `supplierPayableReversed`, `remainingSupplierPayable`, `profitLoss`, `netProfitLoss`, `reason`.

Reversal records include metadata keys: `pnr`, `bookingId`, `reversedAmount`, `remainingSupplierPayable`, `supplierRefundCost`.

**Do not** delete booking PURCHASE ledger rows to “undo” a sale — use this refund path. See [booking-transaction-delete.md](./booking-transaction-delete.md).

---

## Validation errors

| Condition | Error |
|-----------|-------|
| Booking already `REFUND` | Cannot process another admin refund |
| Booking has no price | Cannot process refund |
| PARTIAL without `deductionAmount` | `deductionAmount is required for PARTIAL refund` |
| `deductionAmount` &gt; sell price | Deduction exceeds booking price |
| `supplierRefundCost` &gt; `buyPrice` | supplierRefundCost cannot exceed buy price |
| Missing `reason` | Validation error |
| Supplier reverse failure | Entire refund rolls back (wallet + status) |

---

## UI implementation notes

1. Load booking detail and show **sell price** (`bookingPrice`), **buy price** (`buyPrice`), PNR, ticket.
2. **Customer section**
   - Toggle FULL / PARTIAL
   - If PARTIAL, input `deductionAmount`; show preview: `refund to wallet = sell - deduction`
3. **Supplier section**
   - Required input: `supplierRefundCost` (default `0`)
   - Show preview:
     - `Payable reversed = buyPrice - supplierRefundCost`
     - `Remaining payable for PNR = supplierRefundCost`
4. Require `reason` before submit.
5. On success, display response fields especially `remainingSupplierPayable` and `supplierPayableReversed`.

### Sample admin form layout

```
Customer refund
  ( ) Full refund to agency
  ( ) Partial refund
      Deduction (kept from customer): [____]

Key metrics (USD)
  Sell price:     1,200
  Buy price:        900
  profitLoss:       300   ← original margin (key)

Supplier refund cost (USD)
  Amount supplier keeps: [200.00]
  → Payable reversed: 700
  → Remaining payable (PNR): 200
  → netProfitLoss: 100     ← profitLoss - supplierRefundCost + deduction

Reason: [________________________]

[ Process refund ]
```

---

## Related docs

- [Booking transaction undo (prefer admin refund)](./booking-transaction-delete.md) — do not delete BOOKING ledger rows
- [Booking Pricing & Admin List API](./booking-pricing-admin-api.md) — `buyPrice`, `bookingPrice`, profit/loss fields
- [Ticket Action Admin API](./ticket-action-admin-api.md) — void/cancel/refund via ticket action flow with `supplierRefundCost` on finalize
- [Deposit flow and delete](./deposit-flow-and-delete.md) — wallet deposit cancel/reverse (separate from booking refund)
