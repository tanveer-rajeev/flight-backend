-- Idempotent backfill for legacy flight_info rows (safe if V49 already ran)
UPDATE flight_info
SET leg = 1
WHERE leg IS NULL OR leg < 1;

UPDATE flight_info
SET segment_type = 'ONEWAY'
WHERE segment_type IS NULL OR BTRIM(segment_type) = '';

UPDATE flight_info
SET segment_type = UPPER(BTRIM(segment_type))
WHERE segment_type IS NOT NULL
  AND segment_type <> UPPER(BTRIM(segment_type));
