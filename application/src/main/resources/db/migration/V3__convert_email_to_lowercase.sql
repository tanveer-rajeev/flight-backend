--
-- Migration: Convert all emails to lowercase in user-related tables
-- This ensures case-insensitive email matching for authentication
--

-- Convert emails to lowercase in users table
UPDATE public.users SET email = LOWER(email) WHERE email != LOWER(email);

-- Convert emails to lowercase in admin_users table
UPDATE public.admin_users SET email = LOWER(email) WHERE email != LOWER(email);
