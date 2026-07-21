INSERT INTO public.permissions (is_active, name, slug, module, type)
SELECT true, 'View Activity Log', 'view-activity-log', 'API', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE slug = 'view-activity-log');

INSERT INTO public.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.slug = 'admin' AND r.agency_id IS NULL
  AND p.slug = 'view-activity-log'
  AND NOT EXISTS (
      SELECT 1 FROM public.roles_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
