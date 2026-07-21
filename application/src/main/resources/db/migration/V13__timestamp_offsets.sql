-- Add timezone offset columns for user-local wall-clock timestamps.
-- Existing rows are treated as Asia/Dhaka wall clock.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE users
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

UPDATE users
SET updated_time_offset = 'Asia/Dhaka'
WHERE updated_time_offset IS NULL;

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE booking
SET created_time_offset = COALESCE(time_offset, 'Asia/Dhaka')
WHERE created_time_offset IS NULL;

UPDATE booking
SET updated_time_offset = COALESCE(time_offset, 'Asia/Dhaka')
WHERE updated_time_offset IS NULL;

ALTER TABLE currencies
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE currencies
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

UPDATE currencies
SET updated_time_offset = 'Asia/Dhaka'
WHERE updated_time_offset IS NULL;

ALTER TABLE wallet_deposit
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32);

UPDATE wallet_deposit
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE transactions
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

UPDATE transactions
SET updated_time_offset = 'Asia/Dhaka'
WHERE updated_time_offset IS NULL;

ALTER TABLE ledgers
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE ledgers
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

UPDATE ledgers
SET updated_time_offset = 'Asia/Dhaka'
WHERE updated_time_offset IS NULL;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32);

UPDATE notifications
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

ALTER TABLE support_requests
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE support_requests
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

UPDATE support_requests
SET updated_time_offset = 'Asia/Dhaka'
WHERE updated_time_offset IS NULL;

ALTER TABLE expense
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32),
    ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32);

UPDATE expense
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;

UPDATE expense
SET updated_time_offset = 'Asia/Dhaka'
WHERE updated_time_offset IS NULL;

ALTER TABLE balance_change_history
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32);

UPDATE balance_change_history
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;
