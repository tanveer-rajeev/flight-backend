-- Live chat: conversations + messages (separate from support tickets)
CREATE TABLE IF NOT EXISTS public.chat_conversations (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES public.users (id),
    assigned_admin_id   BIGINT       NULL REFERENCES public.admin_users (id),
    status              VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    subject             VARCHAR(255) NULL,
    last_message_at     TIMESTAMP    NULL,
    closed_at           TIMESTAMP    NULL,
    closed_by_type      VARCHAR(16)  NULL,
    closed_by_id        BIGINT       NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NULL,
    created_time_offset VARCHAR(32)  NULL,
    updated_time_offset VARCHAR(32)  NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_conversations_status_last_msg
    ON public.chat_conversations (status, last_message_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_chat_conversations_user_status
    ON public.chat_conversations (user_id, status);

CREATE INDEX IF NOT EXISTS idx_chat_conversations_assigned_admin
    ON public.chat_conversations (assigned_admin_id)
    WHERE assigned_admin_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.chat_messages (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT       NOT NULL REFERENCES public.chat_conversations (id) ON DELETE CASCADE,
    sender_type         VARCHAR(16)  NOT NULL,
    sender_id           BIGINT       NOT NULL,
    sender_name         VARCHAR(255) NULL,
    body                TEXT         NOT NULL,
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP    NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_time_offset VARCHAR(32)  NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_id
    ON public.chat_messages (conversation_id, id);

CREATE INDEX IF NOT EXISTS idx_chat_messages_unread
    ON public.chat_messages (conversation_id, sender_type, is_read)
    WHERE is_read = FALSE;

-- Permission: manage-live-chat
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Manage Live Chat', 'manage-live-chat', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'manage-live-chat');

INSERT INTO public.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.slug IN ('admin', 'super_admin') AND r.agency_id IS NULL
  AND p.slug = 'manage-live-chat'
  AND NOT EXISTS (
      SELECT 1 FROM public.roles_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
