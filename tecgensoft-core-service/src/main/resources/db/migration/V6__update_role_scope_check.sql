ALTER TABLE public.roles
    DROP CONSTRAINT IF EXISTS roles_scope_check;

ALTER TABLE public.roles
    ADD CONSTRAINT roles_scope_check
        CHECK (scope IN ('PLATFORM', 'ORGANIZATION'));
