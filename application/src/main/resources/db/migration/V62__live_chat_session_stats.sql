-- Track when an admin first joined a conversation (claim or admin-initiated chat).
ALTER TABLE public.chat_conversations
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP NULL;

-- Backfill from first public admin message.
UPDATE public.chat_conversations c
SET claimed_at = sub.first_at
FROM (
    SELECT m.conversation_id, MIN(m.created_at) AS first_at
    FROM public.chat_messages m
    WHERE m.sender_type = 'ADMIN'
      AND COALESCE(m.is_internal, false) = false
    GROUP BY m.conversation_id
) sub
WHERE c.id = sub.conversation_id
  AND c.claimed_at IS NULL;

-- Fallback for assigned threads without a public admin message yet.
UPDATE public.chat_conversations
SET claimed_at = COALESCE(updated_at, created_at)
WHERE claimed_at IS NULL
  AND assigned_admin_id IS NOT NULL
  AND status IN ('ACTIVE', 'CLOSED');

CREATE INDEX IF NOT EXISTS idx_chat_conversations_claimed_at
    ON public.chat_conversations (claimed_at DESC NULLS LAST);
