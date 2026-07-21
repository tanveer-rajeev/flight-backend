-- Fix user_balance_reconciliation:
-- 1. Count only active transactions (is_active = true)
-- 2. Roll child-user transactions up to the mother (wallet) user
-- 3. Align credit/debit type lists with DepositTypeUtil
--
-- Rows are wallet owners only (parent_user_id IS NULL) so agency balance
-- is compared to mother + children txn net in one place.

DROP VIEW IF EXISTS public.user_balance_reconciliation;

CREATE VIEW public.user_balance_reconciliation AS
SELECT
    u.id AS user_id,
    b.company_name AS agency_name,
    round((COALESCE(t.total_credit, 0::double precision))::numeric, 2) AS total_credit,
    round((COALESCE(t.total_debit, 0::double precision))::numeric, 2) AS total_debit,
    round(((COALESCE(t.total_credit, 0::double precision)
            - COALESCE(t.total_debit, 0::double precision)))::numeric, 2) AS deb_cred_balance,
    round((u.balance)::numeric, 2) AS user_balance,
    round((((COALESCE(t.total_credit, 0::double precision)
             - COALESCE(t.total_debit, 0::double precision))
            - u.balance))::numeric, 2) AS diff_amount
FROM public.users u
LEFT JOIN public.businesses b ON b.mother_user_id = u.id
LEFT JOIN (
    SELECT
        COALESCE(owner.parent_user_id, owner.id) AS wallet_user_id,
        sum(
            CASE
                WHEN upper(txn.type::text) = ANY (ARRAY[
                    'DEPOSIT'::text,
                    'BANK_DEPOSIT'::text,
                    'REFUND'::text,
                    'CASH'::text,
                    'CREDIT'::text,
                    'BANK_TRANSFER_OR_MFS'::text,
                    'CHEQUE'::text,
                    'STRIPE'::text,
                    'INSTANT'::text,
                    'NGENIUS'::text,
                    'SSL'::text
                ]) THEN txn.amount
                ELSE 0::double precision
            END
        ) AS total_credit,
        sum(
            CASE
                WHEN upper(txn.type::text) = ANY (ARRAY[
                    'BOOKING_DEDUCTION'::text,
                    'BOOKING_STATUS_UPDATE_DEDUCTION'::text,
                    'DEDUCTION'::text,
                    'WITHDRAWAL'::text,
                    'ADMIN_CHARGE'::text,
                    'PURCHASE'::text
                ]) THEN txn.amount
                ELSE 0::double precision
            END
        ) AS total_debit
    FROM public.transactions txn
    INNER JOIN public.users owner ON owner.id = txn.user_id
    WHERE txn.is_active = true
      AND COALESCE(owner.is_deleted, false) = false
    GROUP BY COALESCE(owner.parent_user_id, owner.id)
) t ON t.wallet_user_id = u.id
WHERE u.parent_user_id IS NULL
  AND COALESCE(u.is_deleted, false) = false;
