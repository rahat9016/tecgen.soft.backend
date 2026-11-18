ALTER TABLE bookings
    ADD COLUMN created_by_user_id UUID,
    ALTER COLUMN customer_name DROP NOT NULL,
    DROP CONSTRAINT IF EXISTS bookings_source_check,
    ADD CONSTRAINT ck_bookings_direct_source CHECK (source IN ('ONLINE', 'PHONE', 'WALK_IN', 'PUBLIC', 'ADMIN'));

CREATE INDEX idx_bookings_org_court_date ON bookings (organization_id, court_id, local_date);
CREATE INDEX idx_bookings_org_customer_phone ON bookings (organization_id, customer_phone);
