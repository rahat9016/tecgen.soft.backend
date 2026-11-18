CREATE TABLE products (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE features (
    id UUID PRIMARY KEY,
    product_code VARCHAR(40) NOT NULL REFERENCES products(code) ON DELETE RESTRICT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    feature_type VARCHAR(12) NOT NULL CHECK (feature_type IN ('BOOLEAN','NUMBER','STRING')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_code, code)
);

CREATE TABLE package_features (
    id UUID PRIMARY KEY,
    package_id UUID NOT NULL REFERENCES packages(id) ON DELETE RESTRICT,
    feature_id UUID NOT NULL REFERENCES features(id) ON DELETE RESTRICT,
    feature_value JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(package_id, feature_id)
);

CREATE TABLE subscription_payments (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    payment_method VARCHAR(32) NOT NULL CHECK (payment_method IN ('BKASH','NAGAD','SSL_COMMERZ','BANK','CASH')),
    payment_status VARCHAR(16) NOT NULL CHECK (payment_status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    transaction_id VARCHAR(160),
    payment_date TIMESTAMPTZ,
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ CHECK (period_start IS NULL OR period_end IS NULL OR period_end > period_start),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_subscription_payments_subscription ON subscription_payments(subscription_id, created_at DESC);

ALTER TABLE subscriptions
    ADD COLUMN is_trial BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN trial_start_date TIMESTAMPTZ,
    ADD COLUMN trial_end_date TIMESTAMPTZ,
    ADD COLUMN activated_at TIMESTAMPTZ,
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE packages ADD COLUMN billing_period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY'
    CHECK (billing_period IN ('MONTHLY','YEARLY'));

ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;
ALTER TABLE subscriptions ADD CONSTRAINT ck_subscriptions_status
    CHECK (status IN ('PENDING','TRIALING','ACTIVE','EXPIRED','SUSPENDED','CANCELLED','REPLACED'));
ALTER TABLE subscriptions ADD CONSTRAINT ck_trial_dates CHECK
    (NOT is_trial OR (trial_start_date IS NOT NULL AND trial_end_date IS NOT NULL AND trial_end_date > trial_start_date));
DROP INDEX IF EXISTS uk_subscriptions_one_active_per_product;
CREATE UNIQUE INDEX uk_subscriptions_one_active_per_product
    ON subscriptions(organization_id, product) WHERE status IN ('ACTIVE','TRIALING');

CREATE TABLE organization_domains (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    hostname VARCHAR(253) NOT NULL UNIQUE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, hostname)
);
CREATE INDEX idx_organization_domains_verified_hostname ON organization_domains(hostname) WHERE verified;

INSERT INTO products (id, code, name, description) VALUES
    ('00000000-0000-7000-8000-000000000001', 'TURF', 'Turf', 'Turf booking management'),
    ('00000000-0000-7000-8000-000000000002', 'HOTEL', 'Hotel', 'Hotel management');

INSERT INTO features(id, product_code, code, name, feature_type) VALUES
('00000000-0000-7000-8000-000000000101','TURF','COURT_MANAGEMENT','Court Management','BOOLEAN'),
('00000000-0000-7000-8000-000000000102','TURF','ONLINE_BOOKING','Online Booking','BOOLEAN'),
('00000000-0000-7000-8000-000000000103','TURF','ADMIN_BOOKING','Admin Booking','BOOLEAN'),
('00000000-0000-7000-8000-000000000104','TURF','BOOKING_MANAGEMENT','Booking Management','BOOLEAN'),
('00000000-0000-7000-8000-000000000105','TURF','PAYMENT_TRACKING','Payment Tracking','BOOLEAN'),
('00000000-0000-7000-8000-000000000106','TURF','ADVANCE_PAYMENT','Advance Payment','BOOLEAN'),
('00000000-0000-7000-8000-000000000107','TURF','MULTIPLE_COURTS','Multiple Courts','BOOLEAN'),
('00000000-0000-7000-8000-000000000108','TURF','REPORTS','Reports','BOOLEAN'),
('00000000-0000-7000-8000-000000000109','TURF','ADVANCED_REPORTS','Advanced Reports','BOOLEAN'),
('00000000-0000-7000-8000-000000000110','TURF','PAYMENT_INTEGRATION','Payment Integration','BOOLEAN'),
('00000000-0000-7000-8000-000000000111','TURF','CUSTOM_DOMAIN','Custom Domain','BOOLEAN'),
('00000000-0000-7000-8000-000000000112','TURF','MAX_COURTS','Maximum Courts','NUMBER'),
('00000000-0000-7000-8000-000000000113','TURF','MAX_BOOKINGS','Maximum Bookings','NUMBER');

INSERT INTO packages (id,code,version,display_name,product,description,currency,price,features,limits)
VALUES ('00000000-0000-7000-8000-000000000201','TURF_BASIC',1,'Basic','TURF',
        'TURF Basic monthly package','BDT',799,
        '["COURT_MANAGEMENT","ONLINE_BOOKING","ADMIN_BOOKING","BOOKING_MANAGEMENT","PAYMENT_TRACKING","ADVANCE_PAYMENT","REPORTS"]'::jsonb,
        '{"MAX_COURTS":2,"MAX_BOOKINGS":-1}'::jsonb)
ON CONFLICT (code,version) DO NOTHING;

INSERT INTO package_features(id,package_id,feature_id,feature_value)
SELECT ('00000000-0000-7000-8000-' || lpad((300 + row_number() OVER (ORDER BY f.code))::text,12,'0'))::uuid,
       p.id,f.id,v.value
FROM packages p
JOIN (VALUES
 ('COURT_MANAGEMENT','true'::jsonb),('ONLINE_BOOKING','true'::jsonb),('ADMIN_BOOKING','true'::jsonb),
 ('BOOKING_MANAGEMENT','true'::jsonb),('PAYMENT_TRACKING','true'::jsonb),('ADVANCE_PAYMENT','true'::jsonb),
 ('MULTIPLE_COURTS','false'::jsonb),('REPORTS','true'::jsonb),('ADVANCED_REPORTS','false'::jsonb),
 ('PAYMENT_INTEGRATION','false'::jsonb),('CUSTOM_DOMAIN','false'::jsonb),('MAX_COURTS','2'::jsonb),
 ('MAX_BOOKINGS','-1'::jsonb)
) AS v(code,value) ON TRUE
JOIN features f ON f.product_code='TURF' AND f.code=v.code
WHERE p.code='TURF_BASIC' AND p.version=1
ON CONFLICT (package_id,feature_id) DO NOTHING;
