-- Live chat: attachments + admin-only internal notes
ALTER TABLE public.chat_messages
    ALTER COLUMN body DROP NOT NULL;

ALTER TABLE public.chat_messages
    ADD COLUMN IF NOT EXISTS attachments TEXT NULL;

ALTER TABLE public.chat_messages
    ADD COLUMN IF NOT EXISTS is_internal BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_chat_messages_public
    ON public.chat_messages (conversation_id, id)
    WHERE is_internal = FALSE;
