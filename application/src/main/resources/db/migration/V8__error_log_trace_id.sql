-- Correlate persisted error logs with API error.traceId / X-Correlation-Id response header.

ALTER TABLE public.error_log
    ADD COLUMN IF NOT EXISTS trace_id character varying(36);

CREATE INDEX IF NOT EXISTS idx_error_log_trace_id
    ON public.error_log (trace_id)
    WHERE trace_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_error_log_user_created
    ON public.error_log (user_id, created_at DESC)
    WHERE user_id IS NOT NULL;
