CREATE INDEX court_organization_active ON courts(organization_id,active);
CREATE INDEX reservation_tenant_court ON reservations(organization_id,branch_id,court_id);
ALTER TABLE venues ADD CHECK(peak_start_hour BETWEEN 0 AND 24),
 ADD CHECK(max_duration_hours BETWEEN 1 AND 24),
 ADD CHECK(booking_window_days BETWEEN 1 AND 365),
 ADD CHECK(cancellation_cutoff_hours BETWEEN 0 AND 720);
ALTER TABLE reservations ADD CHECK(kind<>'HOLD' OR (expires_at IS NOT NULL AND secret_hash IS NOT NULL));
