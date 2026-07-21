ALTER TABLE flight_info
    ADD COLUMN IF NOT EXISTS leg INTEGER,
    ADD COLUMN IF NOT EXISTS segment_type VARCHAR(20);

-- Legacy rows: treat as single-leg one-way (vast majority of existing inventory)
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
