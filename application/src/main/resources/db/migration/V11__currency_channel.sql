ALTER TABLE public.currencies
    ADD COLUMN IF NOT EXISTS channel character varying(255);

ALTER TABLE public.currencies
    DROP CONSTRAINT IF EXISTS uk8s05s130b8kau12dolk9hgmlu;

CREATE UNIQUE INDEX IF NOT EXISTS uk_currencies_code_provider_channel
    ON public.currencies (code, provider_id, COALESCE(channel, ''));
