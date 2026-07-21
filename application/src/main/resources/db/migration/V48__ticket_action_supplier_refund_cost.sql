ALTER TABLE ticket_action_request
    ADD COLUMN supplier_refund_cost numeric(19, 2),
    ADD COLUMN supplier_payable_reversed numeric(19, 2),
    ADD COLUMN remaining_supplier_payable numeric(19, 2);
