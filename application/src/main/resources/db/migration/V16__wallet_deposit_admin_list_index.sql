-- Partial index for admin deposit list (credit types, approved/rejected, sort by created_at)
CREATE INDEX IF NOT EXISTS idx_wallet_deposit_admin_credit_list
    ON public.wallet_deposit (created_at DESC)
    WHERE status IN ('APPROVED', 'REJECTED')
      AND type IN (
          'CASH', 'CHEQUE', 'BANK_DEPOSIT', 'DEPOSIT', 'BANK_TRANSFER_OR_MFS',
          'STRIPE', 'INSTANT', 'PURCHASE', 'NGENIUS', 'SSL'
      );
