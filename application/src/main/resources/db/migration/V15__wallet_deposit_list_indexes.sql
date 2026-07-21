-- Wallet deposit list queries: admin (status + type + sort) and user-scoped pages
CREATE INDEX IF NOT EXISTS idx_wallet_deposit_status_type_created
    ON public.wallet_deposit (status, type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_deposit_user_status_created
    ON public.wallet_deposit (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_deposit_status_currency_created
    ON public.wallet_deposit (status, currency, created_at DESC);

-- Batch agency name resolution for deposit rows
CREATE INDEX IF NOT EXISTS idx_businesses_mother_user_id
    ON public.businesses (mother_user_id);
