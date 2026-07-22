-- Speed up admin dashboard ticket action counts by status/type
CREATE INDEX IF NOT EXISTS idx_ticket_action_request_status
    ON public.ticket_action_request (status);

CREATE INDEX IF NOT EXISTS idx_ticket_action_request_type_status
    ON public.ticket_action_request (type, status);
