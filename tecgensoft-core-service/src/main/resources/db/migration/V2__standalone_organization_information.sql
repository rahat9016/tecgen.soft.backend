ALTER TABLE public.organizations
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS email VARCHAR(254);

DO $$
BEGIN
    IF to_regclass('public.branches') IS NOT NULL THEN
        ALTER TABLE public.branches
            DROP CONSTRAINT IF EXISTS fk_branches_organization;
        ALTER TABLE public.branches
            DROP CONSTRAINT IF EXISTS uk_branch_organization_name;
        ALTER TABLE public.branches
            DROP COLUMN IF EXISTS organization_id;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conrelid = 'public.branches'::regclass
              AND conname = 'uk_branch_name'
        ) THEN
            ALTER TABLE public.branches
                ADD CONSTRAINT uk_branch_name UNIQUE (name);
        END IF;
    END IF;
END $$;
