-- Reissue completion date for ticket action requests (REISSUE type only)
ALTER TABLE public.ticket_action_request
    ADD COLUMN IF NOT EXISTS reissue_date DATE;
