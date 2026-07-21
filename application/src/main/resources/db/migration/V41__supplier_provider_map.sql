CREATE TABLE IF NOT EXISTS public.supplier_provider_mappings (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES public.suppliers(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    channel VARCHAR(100),
    CONSTRAINT uq_supplier_provider_channel UNIQUE (provider, channel)
);

CREATE INDEX IF NOT EXISTS idx_supplier_provider_mappings_supplier_id
    ON public.supplier_provider_mappings (supplier_id);
