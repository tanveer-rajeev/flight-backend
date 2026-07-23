ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS ticketing_time TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS ticketing_time_offset VARCHAR(32);

-- Backfill from the first CONFIRMED timeline event when available.
UPDATE booking b
SET ticketing_time = bt.created_at,
    ticketing_time_offset = COALESCE(b.time_offset, 'Asia/Dhaka')
FROM (
    SELECT booking_id, MIN(created_at) AS created_at
    FROM booking_timeline
    WHERE status = 'CONFIRMED'
    GROUP BY booking_id
) bt
WHERE b.id = bt.booking_id
  AND b.status = 'CONFIRMED'
  AND b.ticketing_time IS NULL;

-- Fallback for confirmed bookings without a timeline row.
UPDATE booking
SET ticketing_time = updated_at,
    ticketing_time_offset = COALESCE(updated_time_offset, time_offset, 'Asia/Dhaka')
WHERE status = 'CONFIRMED'
  AND ticketing_time IS NULL;
