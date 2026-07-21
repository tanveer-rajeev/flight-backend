ALTER TABLE public.deposit_bank
    ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;
