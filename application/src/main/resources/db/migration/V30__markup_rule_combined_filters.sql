ALTER TABLE public.markup_rules
    ADD COLUMN IF NOT EXISTS filter_mode character varying(32) NOT NULL DEFAULT 'INDIVIDUAL';

ALTER TABLE public.markup_rules
    ADD COLUMN IF NOT EXISTS combined_conditions jsonb;

COMMENT ON COLUMN public.markup_rules.filter_mode IS 'INDIVIDUAL = separate route/airline/bookingCode checks; COMBINED = use combined_conditions only';
COMMENT ON COLUMN public.markup_rules.combined_conditions IS 'JSON array of {route, airlineCode, bookingCode} rows; OR across rows, AND within each row';
