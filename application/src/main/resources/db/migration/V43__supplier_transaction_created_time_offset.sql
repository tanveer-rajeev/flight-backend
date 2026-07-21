-- Supplier transaction rows store user-local wall clock; align with booking timestamp model.
ALTER TABLE supplier_transaction_histories
    ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32);

UPDATE supplier_transaction_histories
SET created_time_offset = 'Asia/Dhaka'
WHERE created_time_offset IS NULL;
