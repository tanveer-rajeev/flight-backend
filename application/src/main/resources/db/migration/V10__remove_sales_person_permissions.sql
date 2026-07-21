-- Remove sales-person CRUD permissions (access is full-admin only via application code)

DELETE FROM public.roles_permissions
WHERE permission_id IN (
    SELECT id FROM public.permissions
    WHERE slug IN (
        'admin-create-sales-person',
        'admin-view-sales-person',
        'admin-update-sales-person',
        'admin-delete-sales-person',
        'assign-sales-person-to-business'
    )
);

DELETE FROM public.permissions
WHERE slug IN (
    'admin-create-sales-person',
    'admin-view-sales-person',
    'admin-update-sales-person',
    'admin-delete-sales-person',
    'assign-sales-person-to-business'
);
