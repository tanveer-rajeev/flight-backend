-- Generic transaction source reference (replaces booking_id, deposit_id, payment_id)

ALTER TABLE transactions
    ADD COLUMN source_type VARCHAR(50),
    ADD COLUMN source_id   BIGINT;

UPDATE transactions SET source_type = 'BOOKING', source_id = booking_id
WHERE booking_id IS NOT NULL;

UPDATE transactions SET source_type = 'DEPOSIT', source_id = deposit_id
WHERE deposit_id IS NOT NULL AND source_type IS NULL;

UPDATE transactions SET source_type = 'PAYMENT', source_id = payment_id
WHERE payment_id IS NOT NULL AND source_type IS NULL;

CREATE INDEX idx_transactions_source ON transactions (source_type, source_id);

DROP INDEX IF EXISTS idx_transactions_booking_id;
DROP INDEX IF EXISTS idx_transactions_deposit_id;
DROP INDEX IF EXISTS idx_transactions_pnr;

ALTER TABLE transactions
    DROP COLUMN IF EXISTS booking_id,
    DROP COLUMN IF EXISTS deposit_id,
    DROP COLUMN IF EXISTS payment_id,
    DROP COLUMN IF EXISTS pnr,
    DROP COLUMN IF EXISTS ticket_no;
