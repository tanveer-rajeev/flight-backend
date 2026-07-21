-- Remove orphaned segment_airport rows whose booking_segment no longer exists
DELETE FROM segment_airport sa
WHERE NOT EXISTS (
    SELECT 1 FROM booking_segment bs WHERE bs.id = sa.segment_id
);

-- Remove orphaned segment_airline rows whose booking_segment no longer exists
DELETE FROM segment_airline sa
WHERE NOT EXISTS (
    SELECT 1 FROM booking_segment bs WHERE bs.id = sa.segment_id
);
