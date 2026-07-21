# Admin Booking Edit API

Admin endpoint to correct PNR, sell/buy price, passenger names, and optionally transfer a booking to another agency.

**Append-only:** never deletes original `PURCHASE` rows. Wallet and supplier adjustments create new ledger entries.

Base path: `/api/bookings`

---

## Endpoint

### `PATCH /api/bookings/{id}/admin/edit`

**Permission:** `update-booking-admin`

#### Request

```json
{
  "reason": "Corrected PNR and moved agency",
  "pnr": "XYZ789",
  "bookingPrice": 1300.00,
  "buyPrice": 850.00,
  "travellers": [
    { "travellerId": 55, "title": "MR", "firstName": "Jon", "lastName": "Doh" }
  ],
  "targetUserId": 42
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `reason` | Yes | Audit / timeline reason |
| `pnr` | No | New PNR (duplicates allowed) |
| `bookingPrice` | No | Sell price in **agency currency** (target agency currency if transferring). Stored as USD |
| `buyPrice` | No | Buy price in **agency currency** (target agency currency if transferring). Stored as USD |
| `travellers[]` | No | Updates shared `Traveller` name fields |
| `targetUserId` | No | Mother agency user to transfer ownership to |

At least one of `pnr`, `bookingPrice`, `buyPrice`, `travellers`, `targetUserId` is required.

#### Behaviour

| Change | Effect |
|--------|--------|
| PNR | Updates booking (no uniqueness check) |
| Sell ↑ | Convert agency→USD; append-only PURCHASE delta on current owner wallet (agency currency) |
| Sell ↓ | Convert agency→USD; append-only REFUND credit on current owner wallet |
| Buy change | Convert agency→USD; update `buyPrice` + `profitLoss`; supplier payable ±USD delta |
| Pax name | Updates shared traveller row (affects all bookings using that traveller) |
| Transfer | Ownership move; credit old PURCHASE total; charge new agency in **new** agency currency (new sell input if provided, else stored USD × new rate) |

**Currency rule:** `bookingPrice` / `buyPrice` are always entered in the pricing agency’s currency — current owner, or `targetUserId` agency when transferring. Booking row still stores USD (`bookingPrice` / `buyPrice` / `profitLoss`) and updates `exchangeCurrency` / `exchangeCurrencyRate`.

Blocked when status is `REFUND` or `CANCELLED` / `TICKET_CANCELLED`.

Runs in one `@Transactional` — supplier adjust failure rolls back wallet work.

#### Response (summary)

Returns `AdminBookingEditResponse` with before/after `changes`, wallet deltas, transfer ids, and traveller name changes.

---

## Tests

```bash
./mvnw -Dtest=BookingServiceAdminEditTest test
```

---

## Related

- [Admin Booking Refund](./booking-refund-admin-api.md) — append-only refund (preferred over deletes)
- [Booking transaction undo](./booking-transaction-delete.md) — do not delete BOOKING ledger rows
