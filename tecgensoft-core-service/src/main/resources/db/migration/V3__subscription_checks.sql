ALTER TABLE subscriptions ADD CHECK(status IN ('ACTIVE','SUSPENDED','CANCELLED','REPLACED')),
 ADD CHECK(billing_period IN ('MONTHLY','YEARLY')), ADD CHECK(product='TURF');
CREATE INDEX organization_admin_user ON organizations(admin_user_id);
CREATE INDEX admin_audit_organization ON admin_assignment_audit(organization_id,created_at);
