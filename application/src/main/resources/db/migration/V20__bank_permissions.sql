-- Bank deposit + bank ledger permissions, assigned to global admin role

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Deposit Banks', 'view-bank-deposit', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-bank-deposit');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Deposit Bank', 'create-bank-deposit', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-bank-deposit');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Deposit Bank', 'update-bank-deposit', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-bank-deposit');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Deposit Bank', 'delete-bank-deposit', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-bank-deposit');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Bank Ledger', 'view-bank-ledger', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-bank-ledger');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Bank Withdrawal', 'create-bank-withdrawal', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-bank-withdrawal');

INSERT INTO public.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.slug = 'admin' AND r.agency_id IS NULL
  AND p.slug IN (
      'view-bank-deposit',
      'create-bank-deposit',
      'update-bank-deposit',
      'delete-bank-deposit',
      'view-bank-ledger',
      'create-bank-withdrawal'
  )
  AND NOT EXISTS (
      SELECT 1 FROM public.roles_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
