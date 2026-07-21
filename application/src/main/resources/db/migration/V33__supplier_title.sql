ALTER TABLE public.suppliers
    ADD COLUMN IF NOT EXISTS title character varying(255);

-- Backfill platform channel suppliers with a readable title
UPDATE public.suppliers
SET title = CASE name
    WHEN 'S_BD'            THEN 'Sabre - s-bd'
    WHEN 'ARABIA_PROD'     THEN 'Arabia - arabia-prod'
    WHEN 'GROUP_UAE'       THEN 'Group - group-uae'
    WHEN 'USBANGLAAPI_API' THEN 'UsBangla API - usbangla-api'
    WHEN 'V_BD'            THEN 'Verteil - v-bd'
    WHEN 'FZ_U'            THEN 'FlyDubai - FZ-U'
    WHEN 'GALILEO_BD'      THEN 'Galileo - galileo-bd'
    WHEN 'GALILEO_UAE'     THEN 'Galileo - galileo-uae'
    ELSE title
END
WHERE title IS NULL
  AND email LIKE '%@platform-supplier.local';

-- Legacy archive bucket
UPDATE public.suppliers
SET title = 'Old Supplier (Archived)'
WHERE title IS NULL
  AND email = 'old-supplier@archived.local';
