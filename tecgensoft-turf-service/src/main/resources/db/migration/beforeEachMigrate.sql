-- V3 drops the V1 unique keys on branch_id without dropping the foreign keys that depend on them
-- first, so it always fails on a database built from V1. Remove those foreign keys beforehand, but
-- only while the turf tables are still empty: databases holding data keep their current behaviour
-- and must be migrated deliberately. V3 recreates the organization-only foreign keys.
DO $$
DECLARE
    fk RECORD;
    has_rows BOOLEAN;
BEGIN
    IF to_regclass('public.venues') IS NULL THEN
        RETURN;
    END IF;

    EXECUTE 'SELECT EXISTS (SELECT 1 FROM public.venues)' INTO has_rows;
    IF has_rows THEN
        RETURN;
    END IF;

    FOR fk IN
        SELECT c.conname, c.conrelid::regclass AS table_name
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.contype = 'f'
          AND n.nspname = 'public'
          AND t.relname IN ('courts', 'reservations', 'bookings', 'payments', 'contents')
          AND a.attname = 'branch_id'
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.table_name, fk.conname);
    END LOOP;
END $$;
