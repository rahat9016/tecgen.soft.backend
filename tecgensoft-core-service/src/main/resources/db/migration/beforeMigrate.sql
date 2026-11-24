-- The identity tables predate Flyway: V1-V9 only alter them, so an empty database cannot be
-- migrated on its own (V6 fails on public.roles). Create them once, only on a database that has
-- neither the identity tables nor any recorded migration. Existing databases are left untouched.
-- Organization foreign keys are added by V3.
DO $$
DECLARE
    migrated BOOLEAN := FALSE;
BEGIN
    IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN
        EXECUTE 'SELECT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE success AND version IS NOT NULL)'
            INTO migrated;
    END IF;

    IF migrated
        OR to_regclass('public.users') IS NOT NULL
        OR to_regclass('public.roles') IS NOT NULL
        OR to_regclass('public.branches') IS NOT NULL
        OR to_regclass('public.organizations') IS NOT NULL THEN
        RETURN;
    END IF;

    CREATE TABLE public.users (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ NOT NULL,
        email VARCHAR(150) NOT NULL,
        first_name VARCHAR(120) NOT NULL,
        is_active BOOLEAN NOT NULL,
        last_name VARCHAR(120) NOT NULL,
        password VARCHAR(255) NOT NULL,
        phone VARCHAR(20),
        profile_picture VARCHAR(255),
        salt VARCHAR(255) NOT NULL,
        updated_at TIMESTAMPTZ NOT NULL,
        CONSTRAINT uk_user_email UNIQUE (email)
    );
    CREATE INDEX idx_user_email ON public.users(email);
    CREATE INDEX idx_user_first_name ON public.users(first_name);
    CREATE INDEX idx_user_is_active ON public.users(is_active);
    CREATE INDEX idx_user_last_name ON public.users(last_name);

    CREATE TABLE public.permissions (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ,
        description VARCHAR(1000),
        name VARCHAR(255) NOT NULL UNIQUE,
        updated_at TIMESTAMPTZ
    );

    CREATE TABLE public.roles (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ NOT NULL,
        description TEXT,
        is_active BOOLEAN NOT NULL,
        is_system BOOLEAN NOT NULL,
        name VARCHAR(100) NOT NULL,
        scope VARCHAR(20) NOT NULL,
        updated_at TIMESTAMPTZ NOT NULL,
        organization_id UUID
    );
    CREATE INDEX idx_roles_is_active ON public.roles(is_active);
    CREATE INDEX idx_roles_scope ON public.roles(scope);

    CREATE TABLE public.role_permissions (
        role_id UUID NOT NULL REFERENCES public.roles(id),
        permission_id UUID NOT NULL REFERENCES public.permissions(id),
        CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
    );

    CREATE TABLE public.user_organizations (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ NOT NULL,
        organization_id UUID NOT NULL,
        user_id UUID NOT NULL REFERENCES public.users(id),
        CONSTRAINT uk_user_organization UNIQUE (user_id, organization_id)
    );
    CREATE INDEX idx_user_organizations_organization ON public.user_organizations(organization_id);

    CREATE TABLE public.user_organization_roles (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ NOT NULL,
        organization_id UUID NOT NULL,
        role_id UUID NOT NULL REFERENCES public.roles(id),
        user_id UUID NOT NULL REFERENCES public.users(id),
        CONSTRAINT uk_user_organization_role UNIQUE (user_id, organization_id, role_id)
    );
    CREATE INDEX idx_uor_user_organization ON public.user_organization_roles(user_id, organization_id);
    CREATE INDEX idx_uor_organization_role ON public.user_organization_roles(organization_id, role_id);

    CREATE TABLE public.user_platform_roles (
        id UUID PRIMARY KEY,
        created_at TIMESTAMPTZ NOT NULL,
        updated_at TIMESTAMPTZ NOT NULL,
        role_id UUID NOT NULL REFERENCES public.roles(id),
        user_id UUID NOT NULL REFERENCES public.users(id),
        CONSTRAINT uk_user_platform_role UNIQUE (user_id, role_id)
    );
    CREATE INDEX idx_user_platform_roles_role ON public.user_platform_roles(role_id);
END $$;
