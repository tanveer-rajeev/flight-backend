# Booking transaction undo — prefer admin refund (do not delete)

When undoing a booking wallet charge, **do not delete** `transactions` or `wallet_deposits` rows. Deleting breaks supplier books, reports, and audit history later.

**Prefer:** [`POST /api/bookings/{id}/admin/refund`](./booking-refund-admin-api.md) — append-only refund.

---

## Why not delete a BOOKING transaction?

`DELETE /api/ledger/transaction/{id}` for a `PURCHASE` with `source_type=BOOKING`:

| Reversed | Left inconsistent |
|----------|-------------------|
| Deletes txn + linked deposit by reference | Booking still looks paid/ticketed |
| Restores `users.balance` | Supplier payable / invoices unchanged |
| Maybe removes debit history | Credit limit usage unchanged |
| | No proper refund audit trail |

If a REFUND txn already exists, deleting the PURCHASE can **over-credit** the wallet.

---

## Preferred path: admin refund (append-only)

```
POST /api/bookings/{id}/admin/refund
```

What happens (one DB transaction):

1. **Keeps** original PURCHASE txn/deposit  
2. **Adds** REFUND wallet credit (`transactions` + `wallet_deposits` + balance + history)  
3. Booking status → `REFUND`  
4. **Supplier** payable reversed using `supplierRefundCost` (reversal history row)  
5. Timeline + `BOOKING_REFUNDED` activity audit (`channel=ADMIN_REFUND`)  
6. User notification (best-effort; does not roll back money work)

If supplier reverse fails, the **whole refund rolls back** (wallet + status included).

---

## Supplier side on admin refund

| Field | Meaning |
|-------|---------|
| `supplierRefundCost` | Amount supplier keeps |
| `supplierPayableReversed` | `buyPrice − supplierRefundCost` |
| `remainingSupplierPayable` | Equals `supplierRefundCost` for this PNR |

Writes a **negative** `supplier_transaction_histories` row and reduces `suppliers.payable_amount`. Does not delete the original supplier payable row.

---

## Comparison

| | Admin refund | `deleteTransaction` | `DELETE /api/bookings/{id}` |
|--|--------------|---------------------|----------------------------|
| Original PURCHASE | Kept | Deleted | Deleted |
| Wallet | New REFUND credit | Undo debit | Undo PURCHASE sum |
| Supplier | Reversed | Unchanged | Unchanged |
| Booking | Status `REFUND` | Unchanged | Deleted |
| Audit | `BOOKING_REFUNDED` | None | `BOOKING_DELETED` |
| Safe for history | Yes | No | Risky |

---

## Related

- [Admin Booking Refund API](./booking-refund-admin-api.md)
- [Deposit flow and delete](./deposit-flow-and-delete.md) — deposit cancel/reverse is separate

## Automated tests

| Class | Covers |
|-------|--------|
| [`BookingServiceAdminRefundTest`](../src/test/java/com/example/tufantrip/service/booking/BookingServiceAdminRefundTest.java) | FULL/PARTIAL credit; supplier reverse; append-only (no deletes); already-refunded guard; supplier failure skips audit |
| [`ActivityBookingAuditSupportAdminRefundTest`](../src/test/java/com/example/tufantrip/service/audit/ActivityBookingAuditSupportAdminRefundTest.java) | `BOOKING_REFUNDED` audit metadata (`channel=ADMIN_REFUND`, `appendOnly`) |

```bash
./mvnw -Dtest=BookingServiceAdminRefundTest,ActivityBookingAuditSupportAdminRefundTest test
```
