-- In the organization-only model each former branch becomes its own organization.
-- Re-key operational rows to that former branch UUID before removing branch_id.
DO $$
DECLARE
    table_name TEXT;
    constraint_name TEXT;
    tenant_tables TEXT[] := ARRAY['venues', 'courts', 'reservations', 'bookings', 'payments', 'contents'];
BEGIN
    FOREACH table_name IN ARRAY tenant_tables LOOP
        IF to_regclass('public.' || table_name) IS NOT NULL THEN
            FOR constraint_name IN
                SELECT DISTINCT constraint_row.conname
                FROM pg_constraint constraint_row
                JOIN LATERAL unnest(constraint_row.conkey) AS key_column(attnum) ON TRUE
                JOIN pg_attribute column_row
                  ON column_row.attrelid = constraint_row.conrelid
                 AND column_row.attnum = key_column.attnum
                WHERE constraint_row.conrelid = to_regclass('public.' || table_name)
                  AND column_row.attname = 'branch_id'
            LOOP
                EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I', table_name, constraint_name);
            END LOOP;
        END IF;
    END LOOP;
END $$;

UPDATE venues SET organization_id = branch_id;
ALTER TABLE venues DROP COLUMN branch_id CASCADE;
ALTER TABLE venues ADD CONSTRAINT uk_venues_organization UNIQUE (organization_id);

UPDATE courts SET organization_id = branch_id;
ALTER TABLE courts DROP COLUMN branch_id CASCADE;
ALTER TABLE courts
    ADD CONSTRAINT uk_courts_organization_id UNIQUE (organization_id, id),
    ADD CONSTRAINT fk_courts_venue_organization FOREIGN KEY (organization_id)
        REFERENCES venues(organization_id);

UPDATE reservations SET organization_id = branch_id;
ALTER TABLE reservations DROP COLUMN branch_id CASCADE;
ALTER TABLE reservations
    ADD CONSTRAINT uk_reservations_organization_court_id UNIQUE (organization_id, court_id, id),
    ADD CONSTRAINT fk_reservations_court_organization FOREIGN KEY (organization_id, court_id)
        REFERENCES courts(organization_id, id);

UPDATE bookings SET organization_id = branch_id;
ALTER TABLE bookings DROP COLUMN branch_id CASCADE;
ALTER TABLE bookings
    ADD CONSTRAINT uk_bookings_organization_id UNIQUE (organization_id, id),
    ADD CONSTRAINT fk_bookings_reservation_organization FOREIGN KEY (organization_id, court_id, reservation_id)
        REFERENCES reservations(organization_id, court_id, id);

UPDATE payments SET organization_id = branch_id;
ALTER TABLE payments DROP COLUMN branch_id CASCADE;
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_booking_organization FOREIGN KEY (organization_id, booking_id)
        REFERENCES bookings(organization_id, id);

UPDATE contents SET organization_id = branch_id;
ALTER TABLE contents DROP COLUMN branch_id CASCADE;
ALTER TABLE contents
    ADD CONSTRAINT fk_contents_venue_organization FOREIGN KEY (organization_id)
        REFERENCES venues(organization_id);

CREATE INDEX booking_organization_date ON bookings(organization_id, local_date);
CREATE INDEX payment_organization_booking ON payments(organization_id, booking_id);
CREATE INDEX content_organization_kind ON contents(organization_id, kind);
