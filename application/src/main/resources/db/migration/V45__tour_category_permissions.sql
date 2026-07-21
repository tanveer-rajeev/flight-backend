-- Tour Category permissions
-- API permissions (used by @PreAuthorize in controllers)
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Tour Categories', 'view-tour-category', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-tour-category');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Tour Category', 'create-tour-category', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-tour-category');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Tour Category', 'update-tour-category', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-tour-category');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Tour Category', 'delete-tour-category', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-tour-category');

-- Menu permissions (-m suffix)
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Tour Categories Menu', 'view-tour-category-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-tour-category-m');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Tour Category Menu', 'create-tour-category-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-tour-category-m');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Tour Category Menu', 'update-tour-category-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-tour-category-m');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Tour Category Menu', 'delete-tour-category-m', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-tour-category-m');

-- Menu action permissions (-ma suffix)
INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Tour Categories Menu Action', 'view-tour-category-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-tour-category-ma');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Create Tour Category Menu Action', 'create-tour-category-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'create-tour-category-ma');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Update Tour Category Menu Action', 'update-tour-category-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'update-tour-category-ma');

INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'Delete Tour Category Menu Action', 'delete-tour-category-ma', 'MENU', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'delete-tour-category-ma');

-- Assign all tour category permissions to the super-admin role
INSERT INTO public.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.slug = 'admin' AND r.agency_id IS NULL
  AND p.slug IN (
      'view-tour-category',
      'create-tour-category',
      'update-tour-category',
      'delete-tour-category',
      'view-tour-category-m',
      'create-tour-category-m',
      'update-tour-category-m',
      'delete-tour-category-m',
      'view-tour-category-ma',
      'create-tour-category-ma',
      'update-tour-category-ma',
      'delete-tour-category-ma'
  )
  AND p.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM public.roles_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
