# Deposit flow and how to delete a deposit

Operational guide for wallet deposits: how they work, which tables they touch, and how to cancel or reverse one safely.

Core code:

- [`WalletService.java`](../src/main/java/com/aerionsoft/application/service/wallet/WalletService.java) — `createDeposit`, `approveOrReject`, `adminDeposit`, `deleteTransaction`
- [`WalletController.java`](../src/main/java/com/aerionsoft/application/controller/wallet/WalletController.java)
- [`LedgerController.java`](../src/main/java/com/aerionsoft/application/controller/wallet/LedgerController.java)
- [`BankLedgerService.java`](../src/main/java/com/aerionsoft/application/service/wallet/BankLedgerService.java)

There is **no dedicated delete-deposit API**. Use reject for pending deposits, or ledger transaction delete for approved ones.

---

## How a deposit works

Deposits are request records in `wallet_deposit`. The wallet balance is **`users.balance`** (no separate wallet table). Money is credited only when the deposit becomes **APPROVED**.

```
Create deposit
    → wallet_deposit (PENDING or INIT)
        → Approve / gateway success
            → UPDATE users.balance (+)
            → INSERT transactions
            → INSERT balance_change_history
            → Optional: bank_ledger_entries + deposit_bank.current_balance
            → INSERT activity_log
        → Reject / expire
            → UPDATE wallet_deposit status only
```

### Entry paths

| Path | Create status | When balance changes |
|------|---------------|----------------------|
| `POST /api/wallet/deposit` | `PENDING` | On admin approve |
| `POST /api/wallet/admin/deposit` | `APPROVED` immediately | At create |
| Stripe instant | `PENDING` then approve via webhook | On Stripe success |
| SSLCommerz | `INIT` then approve via callback | On SSL success |
| Tabby / payment completion | `APPROVED` at once | At create |

---

## Tables impacted

### On create only (manual / Stripe checkout / SSL init)

| Table | Change |
|-------|--------|
| `wallet_deposit` | INSERT |
| `ssl_commerz_payments` | INSERT (SSL only) |

No balance change, no `transactions` row.

### On approval / auto-credit

| Table | Change |
|-------|--------|
| `wallet_deposit` | UPDATE to `APPROVED` (or INSERT already approved) |
| `users` | UPDATE `balance` (+amount; parent wallet if child user) |
| `transactions` | INSERT (`source_type=DEPOSIT`, `source_id=deposit.id`) |
| `balance_change_history` | INSERT (`CREDIT`, `reference_type=DEPOSIT`) |
| `bank_ledger_entries` | INSERT if bank-linked (`BANK_DEPOSIT`, `BANK_TRANSFER_OR_MFS`, `CHEQUE`, admin+bank) |
| `deposit_bank` | UPDATE `current_balance` when bank ledger written |
| `activity_log` | INSERT approve/reject/credit audit |
| `ssl_commerz_payments` | UPDATE status (SSL only) |

`wallet_deposit_payment` exists in schema but is unused by services.

Bank ledger is written only when `deposit_bank_id` is set and type is bank-linked (`BankLedgerService.recordAgentDepositApproval`, source type `WALLET_DEPOSIT`).

---

## Step 1 — Confirm deposit status and bank link

Before deleting anything, identify the deposit and whether money / bank ledger was written.

```sql
SELECT
  id,
  user_id,
  type,
  status,
  amount,
  currency,
  reference,
  transaction_id,
  deposit_bank_id,
  approved_at,
  approved_by,
  created_at
FROM wallet_deposit
WHERE id = :depositId;
-- or: WHERE reference = :reference
```

Check linked ledger and bank side effects (APPROVED only):

```sql
-- User ledger transaction
SELECT id, user_id, amount, type, source_type, source_id, reference, active
FROM transactions
WHERE source_type = 'DEPOSIT' AND source_id = :depositId;

-- Balance history
SELECT id, user_id, change_type, amount, reference_type, reference_id, created_at
FROM balance_change_history
WHERE reference_type = 'DEPOSIT' AND reference_id = :depositId;

-- Bank ledger (only if deposit_bank_id was set)
SELECT id, bank_id, entry_type, amount, source_type, source_id, reference, created_at
FROM bank_ledger_entries
WHERE source_type = 'WALLET_DEPOSIT' AND source_id = :depositId;

-- Current bank balance (if bank-linked)
SELECT id, name, current_balance
FROM deposit_bank
WHERE id = (SELECT deposit_bank_id FROM wallet_deposit WHERE id = :depositId);
```

Decision:

| Status | Money credited? | Next step |
|--------|-----------------|-----------|
| `PENDING` / unused `INIT` | No | Step 2 — reject (or hard-delete row only) |
| `APPROVED` | Yes | Step 3 — delete ledger transaction |
| `REJECTED` | No | Already cancelled; optional hard-delete of orphan row only |

---

## Step 2 — PENDING / unused INIT: cancel (prefer reject)

No money moved. Prefer reject so status and audit stay consistent.

**Permission:** `approve-reject-wallet-deposit`

```http
POST /api/wallet/approvals/{depositId}
Content-Type: application/json

{
  "status": "REJECTED",
  "adminRemarks": "Cancelled / duplicate / invalid deposit"
}
```

Effect:

- UPDATE `wallet_deposit.status` → `REJECTED`
- INSERT `activity_log` (`DEPOSIT_REJECTED`)
- **Does not** touch `users.balance`, `transactions`, or bank tables

### Hard-delete only if never credited

Use only when the row must be removed entirely and status is still `PENDING` / `INIT` (never approved):

```sql
-- SSL only, if a session row exists
DELETE FROM ssl_commerz_payments
WHERE payment_type = 'WALLET_DEPOSIT' AND /* link by your deposit/session fields */;

DELETE FROM wallet_deposit WHERE id = :depositId AND status IN ('PENDING', 'INIT');
```

Do **not** change `users.balance` or delete from `transactions` for never-approved deposits.

---

## Step 3 — APPROVED: reverse via ledger transaction delete

Money already credited. Reverse with the existing API (preferred over raw SQL).

**Permission:** `delete-transaction`

1. Find the transaction id from Step 1 (`transactions.id` where `source_type = 'DEPOSIT'` and `source_id = :depositId`).
2. Call:

```http
DELETE /api/ledger/transaction/{transactionId}
```

[`WalletService.deleteTransaction`](../src/main/java/com/aerionsoft/application/service/wallet/WalletService.java) will:

1. Hard-delete linked `wallet_deposit` (by `sourceId` or `reference`)
2. Hard-delete the `transactions` row
3. Reverse `users.balance` (subtract the credit amount; resolves parent wallet for child users)
4. Remove matching `balance_change_history` CREDIT rows (`reference_type=DEPOSIT`, `reference_id=depositId`)

### What this API does **not** reverse

- `bank_ledger_entries`
- `deposit_bank.current_balance`
- `activity_log`
- `ssl_commerz_payments`

Continue to Step 4 if the deposit was bank-linked.

---

## Step 4 — Bank ledger gap (manual)

If Step 1 found a `bank_ledger_entries` row with `source_type = 'WALLET_DEPOSIT'` and `source_id = :depositId`, fix bank books after the ledger transaction delete.

There is no bank-ledger reverse API today. Do this in a single DB transaction:

```sql
-- Capture amount and bank before delete
SELECT id, bank_id, amount, entry_type
FROM bank_ledger_entries
WHERE source_type = 'WALLET_DEPOSIT' AND source_id = :depositId;

-- Example: CREDIT entry of :amount on bank :bankId
UPDATE deposit_bank
SET current_balance = current_balance - :amount
WHERE id = :bankId;

DELETE FROM bank_ledger_entries
WHERE source_type = 'WALLET_DEPOSIT' AND source_id = :depositId;
```

If `entry_type` was `DEBIT` instead of `CREDIT`, add the amount back to `current_balance` instead of subtracting.

Optionally leave `activity_log` rows for audit, or archive them separately. Optionally clean `ssl_commerz_payments` for SSL deposits.

---

## Step 5 — Verify consistency

After reject or reverse, confirm no leftover credit side effects:

```sql
-- Deposit should be REJECTED (cancel) or gone (approved reverse)
SELECT id, status FROM wallet_deposit WHERE id = :depositId;

-- No leftover user ledger
SELECT id FROM transactions
WHERE source_type = 'DEPOSIT' AND source_id = :depositId;

-- No leftover balance history credit
SELECT id FROM balance_change_history
WHERE reference_type = 'DEPOSIT' AND reference_id = :depositId AND change_type = 'CREDIT';

-- No leftover bank ledger
SELECT id FROM bank_ledger_entries
WHERE source_type = 'WALLET_DEPOSIT' AND source_id = :depositId;

-- Wallet balance matches expectation after reverse
SELECT id, balance FROM users WHERE id = :walletUserId;
```

Expected outcomes:

| Action taken | `wallet_deposit` | `transactions` | `users.balance` | `balance_change_history` | bank tables |
|--------------|------------------|----------------|-----------------|--------------------------|-------------|
| Reject PENDING | `REJECTED` | none | unchanged | none | unchanged |
| Delete APPROVED txn | row deleted | row deleted | reduced by amount | CREDIT removed | must be fixed manually if bank-linked |

---

## Quick checklist

1. Confirm deposit `id`, `status`, `amount`, `user_id`, optional `deposit_bank_id`, and related rows (Step 1).
2. **PENDING / INIT:** `POST /api/wallet/approvals/{id}` with `REJECTED` (Step 2). Hard-delete only if never credited.
3. **APPROVED:** find `transactions` row, then `DELETE /api/ledger/transaction/{transactionId}` (Step 3).
4. **Bank-linked:** reverse/remove `bank_ledger_entries` and adjust `deposit_bank.current_balance` (Step 4).
5. **Verify** balance, history, deposit, and bank rows (Step 5).
6. Optionally leave `activity_log` as the audit trail.

---

## Automated tests

| Class | Covers |
|-------|--------|
| [`WalletServiceDepositDeleteTest`](../src/test/java/com/example/tufantrip/service/wallet/WalletServiceDepositDeleteTest.java) | Approve status side effects; PENDING reject (no credit); APPROVED `deleteTransaction` reverse; bank-ledger gap (not reversed by API); consistency of history cleanup |
| [`BankLedgerServiceDepositApprovalTest`](../src/test/java/com/example/tufantrip/service/wallet/BankLedgerServiceDepositApprovalTest.java) | Bank-linked approval writes `WALLET_DEPOSIT` ledger; cash skips bank; idempotency; manual bank reverse after wallet delete |

```bash
./mvnw -Dtest=WalletServiceDepositDeleteTest,BankLedgerServiceDepositApprovalTest test
```

---

## Scope note

This document describes current deposit cancel/reverse behavior. A dedicated `DELETE /api/wallet/deposits/{id}` that handles PENDING reject vs APPROVED full reversal (including bank ledger) is not implemented yet.

## Related

- [Booking transaction undo — prefer admin refund](./booking-transaction-delete.md) (do not delete BOOKING ledger rows)
