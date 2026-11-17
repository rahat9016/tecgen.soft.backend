ALTER TABLE public.organizations
    ADD COLUMN IF NOT EXISTS code VARCHAR(40),
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255);

UPDATE public.organizations
SET code = 'ORG_' || upper(substr(replace(id::text, '-', ''), 1, 12))
WHERE code IS NULL;

DO $$
DECLARE
    old_organization RECORD;
    old_permission RECORD;
    migrated_name VARCHAR(255);
    migrated_code VARCHAR(40);
    target_permission_id UUID;
    target_name VARCHAR(255);
BEGIN
    IF to_regclass('public.branches') IS NOT NULL THEN
        FOR old_organization IN SELECT * FROM public.branches LOOP
            migrated_name := old_organization.name;
            IF EXISTS (
                SELECT 1 FROM public.organizations
                WHERE name = migrated_name AND id <> old_organization.id
            ) THEN
                migrated_name := left(old_organization.name, 220) || ' (' || old_organization.code || ')';
            END IF;

            migrated_code := 'ORG_' || upper(regexp_replace(old_organization.code, '[^A-Za-z0-9]', '', 'g'));
            migrated_code := left(migrated_code, 40);

            INSERT INTO public.organizations
                (id, name, code, logo_url, address, phone, email, is_active, created_at, updated_at)
            VALUES
                (old_organization.id, migrated_name, migrated_code, old_organization.logo_url,
                 old_organization.address, old_organization.phone, NULL, old_organization.is_active,
                 old_organization.created_at, old_organization.updated_at)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                code = EXCLUDED.code,
                logo_url = EXCLUDED.logo_url,
                address = EXCLUDED.address,
                phone = EXCLUDED.phone,
                is_active = EXCLUDED.is_active,
                updated_at = EXCLUDED.updated_at;
        END LOOP;
    END IF;
END $$;

ALTER TABLE public.organizations ALTER COLUMN code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_organizations_code ON public.organizations(code);

DO $$
BEGIN
    IF to_regclass('public.roles') IS NOT NULL THEN
        ALTER TABLE public.roles ADD COLUMN IF NOT EXISTS organization_id UUID;
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'roles' AND column_name = 'branch_id'
        ) THEN
            UPDATE public.roles SET organization_id = branch_id WHERE branch_id IS NOT NULL;
            ALTER TABLE public.roles DROP COLUMN branch_id CASCADE;
        END IF;
        UPDATE public.roles SET scope = 'ORGANIZATION' WHERE scope = 'BRANCH';
        UPDATE public.roles SET name = 'ORGANIZATION_ADMIN' WHERE name = 'BRANCH_ADMIN';
        CREATE INDEX IF NOT EXISTS idx_roles_organization ON public.roles(organization_id);
    END IF;

    IF to_regclass('public.user_branches') IS NOT NULL THEN
        ALTER TABLE public.user_branches RENAME TO user_organizations;
        ALTER TABLE public.user_organizations RENAME COLUMN branch_id TO organization_id;
        ALTER TABLE public.user_organizations RENAME CONSTRAINT uk_user_branch TO uk_user_organization;
        ALTER INDEX IF EXISTS public.idx_user_branches_branch RENAME TO idx_user_organizations_organization;
    END IF;

    IF to_regclass('public.user_branch_roles') IS NOT NULL THEN
        ALTER TABLE public.user_branch_roles RENAME TO user_organization_roles;
        ALTER TABLE public.user_organization_roles RENAME COLUMN branch_id TO organization_id;
        ALTER TABLE public.user_organization_roles RENAME CONSTRAINT uk_user_branch_role TO uk_user_organization_role;
        ALTER INDEX IF EXISTS public.idx_ubr_user_branch RENAME TO idx_uor_user_organization;
        ALTER INDEX IF EXISTS public.idx_ubr_branch_role RENAME TO idx_uor_organization_role;
    END IF;

    IF to_regclass('public.permissions') IS NOT NULL THEN
        IF to_regclass('public.role_permissions') IS NOT NULL THEN
            FOR old_permission IN
                SELECT id, name FROM public.permissions WHERE name LIKE 'BRANCH_%'
            LOOP
                target_name := replace(old_permission.name, 'BRANCH_', 'ORGANIZATION_');
                SELECT id INTO target_permission_id FROM public.permissions WHERE name = target_name;
                IF target_permission_id IS NOT NULL THEN
                    INSERT INTO public.role_permissions(role_id, permission_id)
                    SELECT rp.role_id, target_permission_id
                    FROM public.role_permissions rp
                    WHERE rp.permission_id = old_permission.id
                      AND NOT EXISTS (
                          SELECT 1 FROM public.role_permissions existing
                          WHERE existing.role_id = rp.role_id
                            AND existing.permission_id = target_permission_id
                      );
                    DELETE FROM public.role_permissions WHERE permission_id = old_permission.id;
                    DELETE FROM public.permissions WHERE id = old_permission.id;
                ELSE
                    UPDATE public.permissions
                    SET name = target_name,
                        description = replace(replace(description, 'branch', 'organization'), 'Branch', 'Organization')
                    WHERE id = old_permission.id;
                END IF;
            END LOOP;
        ELSE
            UPDATE public.permissions SET name = replace(name, 'BRANCH_', 'ORGANIZATION_')
            WHERE name LIKE 'BRANCH_%';
        END IF;
        UPDATE public.permissions SET description = replace(replace(description, 'branch', 'organization'), 'Branch', 'Organization');
    END IF;

    IF to_regclass('public.branches') IS NOT NULL THEN
        DROP TABLE public.branches CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.user_organizations') IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_organizations_organization'
    ) THEN
        ALTER TABLE public.user_organizations
            ADD CONSTRAINT fk_user_organizations_organization
            FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
    END IF;
    IF to_regclass('public.user_organization_roles') IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_organization_roles_organization'
    ) THEN
        ALTER TABLE public.user_organization_roles
            ADD CONSTRAINT fk_user_organization_roles_organization
            FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
    END IF;
    IF to_regclass('public.roles') IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_roles_organization'
    ) THEN
        ALTER TABLE public.roles
            ADD CONSTRAINT fk_roles_organization
            FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
    END IF;
END $$;
