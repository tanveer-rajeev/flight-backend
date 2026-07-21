ALTER TABLE booking ADD COLUMN IF NOT EXISTS profit_loss character varying(255);

UPDATE booking
SET profit_loss = (
    booking_price::numeric - COALESCE(NULLIF(TRIM(buy_price), '')::numeric, original_price::numeric)
)::varchar
WHERE profit_loss IS NULL
  AND booking_price IS NOT NULL
  AND TRIM(booking_price) <> ''
  AND original_price IS NOT NULL
  AND TRIM(original_price) <> '';
