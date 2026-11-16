CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
DECLARE
    existing_constraint RECORD;
BEGIN
    IF to_regclass('public.branches') IS NOT NULL THEN
        ALTER TABLE public.branches
            ADD COLUMN IF NOT EXISTS organization_id UUID;

        IF EXISTS (
            SELECT 1 FROM public.branches WHERE organization_id IS NULL
        ) THEN
            INSERT INTO public.organizations (id, name)
            VALUES (gen_random_uuid(), 'Legacy Organization')
            ON CONFLICT (name) DO NOTHING;

            UPDATE public.branches
            SET organization_id = (
                SELECT id FROM public.organizations
                WHERE name = 'Legacy Organization'
            )
            WHERE organization_id IS NULL;
        END IF;

        FOR existing_constraint IN
            SELECT c.conname
            FROM pg_constraint c
            WHERE c.conrelid = 'public.branches'::regclass
              AND c.contype = 'u'
              AND array_length(c.conkey, 1) = 1
              AND (
                  SELECT a.attname
                  FROM pg_attribute a
                  WHERE a.attrelid = c.conrelid
                    AND a.attnum = c.conkey[1]
              ) = 'name'
        LOOP
            EXECUTE format(
                'ALTER TABLE public.branches DROP CONSTRAINT %I',
                existing_constraint.conname
            );
        END LOOP;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conrelid = 'public.branches'::regclass
              AND conname = 'fk_branches_organization'
        ) THEN
            ALTER TABLE public.branches
                ADD CONSTRAINT fk_branches_organization
                FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
        END IF;

        ALTER TABLE public.branches
            ALTER COLUMN organization_id SET NOT NULL;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conrelid = 'public.branches'::regclass
              AND conname = 'uk_branch_organization_name'
        ) THEN
            ALTER TABLE public.branches
                ADD CONSTRAINT uk_branch_organization_name
                UNIQUE (organization_id, name);
        END IF;
    END IF;
END $$;
