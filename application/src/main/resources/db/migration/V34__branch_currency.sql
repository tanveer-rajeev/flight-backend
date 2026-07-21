ALTER TABLE public.branches
    ADD COLUMN IF NOT EXISTS currency character varying(3) NOT NULL DEFAULT 'USD';
