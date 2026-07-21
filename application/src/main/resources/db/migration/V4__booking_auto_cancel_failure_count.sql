-- Tracks failed auto-cancel attempts by BookingCancelScheduler (max 3 per booking).
ALTER TABLE public.booking
    ADD COLUMN IF NOT EXISTS auto_cancel_failure_count integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN public.booking.auto_cancel_failure_count IS
    'Failed auto-cancel attempts from BookingCancelScheduler; stops retrying at 3';
