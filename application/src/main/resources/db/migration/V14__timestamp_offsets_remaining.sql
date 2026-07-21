-- Add created_time_offset / updated_time_offset to tables with timestamp columns.
-- Skips partition child tables (ALTER parent partitioned table propagates to partitions).

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'created_at'
          AND NOT EXISTS (
              SELECT 1
              FROM information_schema.columns c2
              WHERE c2.table_schema = 'public'
                AND c2.table_name = c.table_name
                AND c2.column_name = 'created_time_offset'
          )
          AND NOT EXISTS (
              SELECT 1
              FROM pg_inherits inh
              JOIN pg_class child ON child.oid = inh.inhrelid
              JOIN pg_namespace ns ON ns.oid = child.relnamespace
              WHERE ns.nspname = 'public'
                AND child.relname = c.table_name
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32)',
            r.table_name
        );
        EXECUTE format(
            'UPDATE %I SET created_time_offset = ''Asia/Dhaka'' WHERE created_time_offset IS NULL',
            r.table_name
        );
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'updated_at'
          AND NOT EXISTS (
              SELECT 1
              FROM information_schema.columns c2
              WHERE c2.table_schema = 'public'
                AND c2.table_name = c.table_name
                AND c2.column_name = 'updated_time_offset'
          )
          AND NOT EXISTS (
              SELECT 1
              FROM pg_inherits inh
              JOIN pg_class child ON child.oid = inh.inhrelid
              JOIN pg_namespace ns ON ns.oid = child.relnamespace
              WHERE ns.nspname = 'public'
                AND child.relname = c.table_name
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32)',
            r.table_name
        );
        EXECUTE format(
            'UPDATE %I SET updated_time_offset = ''Asia/Dhaka'' WHERE updated_time_offset IS NULL',
            r.table_name
        );
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'create_at'
          AND NOT EXISTS (
              SELECT 1
              FROM information_schema.columns c2
              WHERE c2.table_schema = 'public'
                AND c2.table_name = c.table_name
                AND c2.column_name = 'created_time_offset'
          )
          AND NOT EXISTS (
              SELECT 1
              FROM pg_inherits inh
              JOIN pg_class child ON child.oid = inh.inhrelid
              JOIN pg_namespace ns ON ns.oid = child.relnamespace
              WHERE ns.nspname = 'public'
                AND child.relname = c.table_name
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_time_offset VARCHAR(32)',
            r.table_name
        );
        EXECUTE format(
            'UPDATE %I SET created_time_offset = ''Asia/Dhaka'' WHERE created_time_offset IS NULL',
            r.table_name
        );
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'update_at'
          AND NOT EXISTS (
              SELECT 1
              FROM information_schema.columns c2
              WHERE c2.table_schema = 'public'
                AND c2.table_name = c.table_name
                AND c2.column_name = 'updated_time_offset'
          )
          AND NOT EXISTS (
              SELECT 1
              FROM pg_inherits inh
              JOIN pg_class child ON child.oid = inh.inhrelid
              JOIN pg_namespace ns ON ns.oid = child.relnamespace
              WHERE ns.nspname = 'public'
                AND child.relname = c.table_name
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_time_offset VARCHAR(32)',
            r.table_name
        );
        EXECUTE format(
            'UPDATE %I SET updated_time_offset = ''Asia/Dhaka'' WHERE updated_time_offset IS NULL',
            r.table_name
        );
    END LOOP;
END $$;
