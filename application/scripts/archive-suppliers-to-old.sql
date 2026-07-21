-- =============================================================================
-- Archive all suppliers into one "OLD SUPPLIER" row, then delete the rest.
--
-- What this does:
--   1. Creates (or reuses) a single admin-global supplier: OLD SUPPLIER
--   2. Reassigns invoice_items, supplier_transaction_histories, group_tickets
--   3. Rolls up paid_amount / payable_amount onto OLD SUPPLIER
--   4. Deletes every other supplier row
--
-- What this does NOT delete:
--   - invoices, invoice_items, invoice_dynamic_items
--   - supplier_transaction_histories / details
--   - group_tickets (only supplier_id is changed)
--
-- Run against PostgreSQL. Review the preview queries first.
-- RECOMMENDED: run inside a transaction and COMMIT only after verifying counts.
-- =============================================================================

-- ── Preview (run before migration) ───────────────────────────────────────────

-- SELECT COUNT(*) AS supplier_count FROM suppliers;
-- SELECT COUNT(*) AS invoice_items FROM invoice_items;
-- SELECT COUNT(*) AS txn_histories FROM supplier_transaction_histories WHERE supplier_id IS NOT NULL;
-- SELECT COUNT(*) AS group_tickets_with_supplier FROM group_tickets WHERE supplier_id IS NOT NULL;

-- SELECT s.id, s.name, s.email, s.agency_id,
--        (SELECT COUNT(*) FROM invoice_items ii WHERE ii.supplier_id = s.id) AS invoice_items,
--        (SELECT COUNT(*) FROM supplier_transaction_histories sth WHERE sth.supplier_id = s.id) AS histories
-- FROM suppliers s
-- ORDER BY s.id;

BEGIN;

-- ── 1. Create or reuse OLD SUPPLIER ──────────────────────────────────────────

DO $$
DECLARE
    v_old_supplier_id bigint;
    v_total_paid      numeric(19,4);
    v_total_payable   numeric(19,4);
    v_old_email       text := 'old-supplier@archived.local';
BEGIN
    SELECT id INTO v_old_supplier_id
    FROM suppliers
    WHERE email = v_old_email
    LIMIT 1;

    IF v_old_supplier_id IS NULL THEN
        INSERT INTO suppliers (
            name,
            email,
            address,
            phone_number,
            description,
            created_by,
            create_at,
            is_deleted,
            paid_amount,
            payable_amount,
            agency_id,
            branch_id
        ) VALUES (
            'OLD SUPPLIER',
            v_old_email,
            'N/A',
            '0000000000',
            'Archived bucket supplier — all legacy suppliers merged here',
            1,
            now(),
            false,
            0,
            0,
            NULL,
            NULL
        )
        RETURNING id INTO v_old_supplier_id;

        RAISE NOTICE 'Created OLD SUPPLIER id=%', v_old_supplier_id;
    ELSE
        RAISE NOTICE 'Reusing existing OLD SUPPLIER id=%', v_old_supplier_id;
    END IF;

    -- ── 2. Roll up balances from suppliers that will be removed ─────────────

    SELECT
        COALESCE(SUM(paid_amount), 0),
        COALESCE(SUM(payable_amount), 0)
    INTO v_total_paid, v_total_payable
    FROM suppliers
    WHERE id <> v_old_supplier_id;

    UPDATE suppliers
    SET paid_amount    = v_total_paid,
        payable_amount = v_total_payable,
        update_at      = now(),
        updated_by     = 1
    WHERE id = v_old_supplier_id;

    -- ── 3. Reassign all foreign references ───────────────────────────────────

    UPDATE invoice_items
    SET supplier_id = v_old_supplier_id
    WHERE supplier_id IS DISTINCT FROM v_old_supplier_id;

    UPDATE supplier_transaction_histories
    SET supplier_id = v_old_supplier_id
    WHERE supplier_id IS DISTINCT FROM v_old_supplier_id;

    UPDATE group_tickets
    SET supplier_id = v_old_supplier_id
    WHERE supplier_id IS DISTINCT FROM v_old_supplier_id;

    -- ── 4. Delete all other suppliers ─────────────────────────────────────────

    DELETE FROM suppliers
    WHERE id <> v_old_supplier_id;

    RAISE NOTICE 'Done. Only supplier id=% (OLD SUPPLIER) remains.', v_old_supplier_id;
END $$;

-- ── Verify (must show 1 supplier) ────────────────────────────────────────────

SELECT id, name, email, paid_amount, payable_amount, agency_id, branch_id
FROM suppliers;

SELECT COUNT(*) AS remaining_suppliers FROM suppliers;

SELECT supplier_id, COUNT(*) AS invoice_item_count
FROM invoice_items
GROUP BY supplier_id;

SELECT supplier_id, COUNT(*) AS history_count
FROM supplier_transaction_histories
WHERE supplier_id IS NOT NULL
GROUP BY supplier_id;

-- If everything looks correct:
COMMIT;

-- If something is wrong:
-- ROLLBACK;
