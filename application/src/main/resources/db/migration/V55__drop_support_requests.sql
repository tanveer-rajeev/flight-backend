-- Remove legacy support-request feature (replaced by live chat)
DROP TABLE IF EXISTS public.support_messages;
DROP TABLE IF EXISTS public.support_requests;
