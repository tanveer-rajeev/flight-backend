-- V2: Critical and High priority indexes
-- See docs/database-index-analysis.md for rationale and query mapping.

-- =============================================================================
-- CRITICAL: booking (core transactional table, no secondary indexes in baseline)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_booking_created_by_created_at
    ON public.booking (created_by, created_at DESC);

-- PNR is not unique; composite supports multiple rows per PNR (newest first)
CREATE INDEX IF NOT EXISTS idx_booking_pnr_created_at
    ON public.booking (pnr, created_at DESC)
    WHERE pnr IS NOT NULL AND pnr <> '';

CREATE INDEX IF NOT EXISTS idx_booking_status_provider
    ON public.booking (status, provider_name);

-- =============================================================================
-- CRITICAL: notifications (inbox polled on every authenticated session)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_notifications_user_status_created
    ON public.notifications (user_id, status, created_at DESC);

-- =============================================================================
-- CRITICAL: wallet_deposit (financial ledger — user history + admin reports)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_wallet_deposit_user_created
    ON public.wallet_deposit (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_deposit_status_created
    ON public.wallet_deposit (status, created_at DESC);

-- =============================================================================
-- CRITICAL: refresh_tokens (client auth — admin side already indexed in V1)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked
    ON public.refresh_tokens (user_id, revoked);

-- =============================================================================
-- CRITICAL: role_assignments (RBAC on protected routes)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_role_assignments_entity
    ON public.role_assignments (entity_type, entity_id);

-- =============================================================================
-- CRITICAL: travel_information (FK join on every booking detail load)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_travel_information_booking_id
    ON public.travel_information (booking_id);

-- =============================================================================
-- CRITICAL: users (agency hierarchy resolution)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_users_business_id
    ON public.users (business_id);

CREATE INDEX IF NOT EXISTS idx_users_parent_user_id
    ON public.users (parent_user_id);

-- =============================================================================
-- HIGH: booking (admin dashboards, status counts, schedulers)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_booking_created_at
    ON public.booking (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_booking_status
    ON public.booking (status);

-- =============================================================================
-- HIGH: wallet_deposit (parent/child agency OR queries)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_wallet_deposit_acting_user_id
    ON public.wallet_deposit (acting_user_id);

-- =============================================================================
-- HIGH: payments, booking segments, travellers, login history
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_payments_booking_id
    ON public.payments (booking_id);

CREATE INDEX IF NOT EXISTS idx_booking_segment_travel_info_id
    ON public.booking_segment (travel_information_id);

CREATE INDEX IF NOT EXISTS idx_travellers_created_by
    ON public.travellers (created_by);

CREATE INDEX IF NOT EXISTS idx_login_history_login_at
    ON public.login_history (login_at DESC);

CREATE INDEX IF NOT EXISTS idx_login_history_user_login_at
    ON public.login_history (user_id, login_at DESC);
