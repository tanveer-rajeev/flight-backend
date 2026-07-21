-- =============================================================================
-- Seed platform channel suppliers (matches booking invoice resolution).
--
-- Supplier name = UPPER(channel with '-' and spaces replaced by '_')
-- Supplier title = human-readable label for UI
--
-- Run after archive-suppliers-to-old.sql if you consolidated legacy suppliers.
-- Idempotent: skips rows that already exist by name or email.
-- =============================================================================

INSERT INTO suppliers (
    name, title, email, address, phone_number, description,
    created_by, create_at, is_deleted, paid_amount, payable_amount, agency_id, branch_id
)
SELECT v.name, v.title, v.email, 'N/A', '0000000000', v.description,
       1, now(), false, 0, 0, NULL, NULL
FROM (VALUES
    ('S_BD',            'Sabre - s-bd',              's_bd@platform-supplier.local',            'Platform channel supplier: SABRE / s-bd'),
    ('ARABIA_PROD',     'Arabia - arabia-prod',      'arabia_prod@platform-supplier.local',     'Platform channel supplier: ARABIA / arabia-prod'),
    ('GROUP_UAE',       'Group - group-uae',         'group_uae@platform-supplier.local',       'Platform channel supplier: GROUP / group-uae'),
    ('USBANGLAAPI_API', 'Usbanglaapi - usbangla-api','usbanglaapi_api@platform-supplier.local', 'Platform channel supplier: USBANGLAAPI / usbangla-api'),
    ('V_BD',            'Verteil - v-bd',            'v_bd@platform-supplier.local',            'Platform channel supplier: VERTEIL / v-bd'),
    ('FZ_U',            'Flydubai - FZ-U',           'fz_u@platform-supplier.local',            'Platform channel supplier: FLYDUBAI / FZ-U'),
    ('GALILEO_BD',      'Galileo - galileo-bd',      'galileo_bd@platform-supplier.local',      'Platform channel supplier: GALILEO / galileo-bd'),
    ('GALILEO_UAE',     'Galileo - galileo-uae',     'galileo_uae@platform-supplier.local',     'Platform channel supplier: GALILEO / galileo-uae')
) AS v(name, title, email, description)
WHERE NOT EXISTS (
    SELECT 1 FROM suppliers s WHERE s.name = v.name OR s.email = v.email
);

SELECT id, name, title, email, description FROM suppliers
WHERE email LIKE '%@platform-supplier.local'
ORDER BY name;
