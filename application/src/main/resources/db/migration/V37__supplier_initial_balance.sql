ALTER TABLE public.suppliers
    ADD COLUMN IF NOT EXISTS initial_balance numeric(19, 4) NOT NULL DEFAULT 0;
