ALTER TABLE public.markup_rules
    RENAME COLUMN cabin_classes TO booking_codes;

COMMENT ON COLUMN public.markup_rules.booking_codes IS 'Comma-separated booking/fare class codes (e.g. Y,M,B) for individual filter mode';

COMMENT ON COLUMN public.markup_rules.combined_conditions IS 'JSON array of {route, airlineCode, bookingCode} rows; OR across rows, AND within each row';
