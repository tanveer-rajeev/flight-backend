-- Fix tour package type permissions if V27 was applied with incorrect -ma API slugs

-- Convert incorrectly seeded -ma permissions from API → MENU
UPDATE public.permissions SET module = 'MENU', is_active = true
WHERE slug IN (
    'view-tour-package-type-ma',
    'create-tour-package-type-ma',
    'update-tour-package-type-ma',
    'delete-tour-package-type-ma'
)
  AND module = 'API';

-- Deactivate obsolete standalone menu slug from first V27 draft
UPDATE public.permissions SET is_active = false WHERE slug = 'tour-package-type-m';
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Tour Package Types', 'view-tour-package-type', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-tour-package-type');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Tour Package Type', 'create-tour-package-type', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-tour-package-type');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Tour Package Type', 'update-tour-package-type', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-tour-package-type');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Tour Package Type', 'delete-tour-package-type', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-tour-package-type');

-- Menu permissions (-m suffix)
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Tour Package Types Menu', 'view-tour-package-type-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-tour-package-type-m');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Tour Package Type Menu', 'create-tour-package-type-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-tour-package-type-m');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Tour Package Type Menu', 'update-tour-package-type-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-tour-package-type-m');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Tour Package Type Menu', 'delete-tour-package-type-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-tour-package-type-m');

-- Menu action permissions (-ma suffix)
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Tour Package Types Menu Action', 'view-tour-package-type-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-tour-package-type-ma');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Tour Package Type Menu Action', 'create-tour-package-type-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-tour-package-type-ma');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Tour Package Type Menu Action', 'update-tour-package-type-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-tour-package-type-ma');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Tour Package Type Menu Action', 'delete-tour-package-type-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-tour-package-type-ma');

INSERT INTO public.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.slug = 'admin' AND r.agency_id IS NULL
  AND p.slug IN (
      'view-tour-package-type',
      'create-tour-package-type',
      'update-tour-package-type',
      'delete-tour-package-type',
      'view-tour-package-type-m',
      'view-tour-package-type-ma',
      'create-tour-package-type-m',
      'create-tour-package-type-ma',
      'update-tour-package-type-m',
      'update-tour-package-type-ma',
      'delete-tour-package-type-m',
      'delete-tour-package-type-ma'
  )
  AND p.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM public.roles_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
