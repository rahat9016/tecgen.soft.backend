CREATE TABLE packages (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    display_name VARCHAR(120) NOT NULL,
    product VARCHAR(30) NOT NULL CHECK (product IN ('TURF', 'HOTEL')),
    description TEXT,
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    features JSONB NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(features) = 'array'),
    limits JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(limits) = 'object'),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    available_for_sale BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_package_code_version UNIQUE (code, version),
    CONSTRAINT uk_package_id_product UNIQUE (id, product)
);

CREATE INDEX idx_packages_product_active
    ON packages(product, active, available_for_sale);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    package_id UUID NOT NULL,
    product VARCHAR(30) NOT NULL CHECK (product IN ('TURF', 'HOTEL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'REPLACED')),
    billing_period VARCHAR(20) NOT NULL CHECK (billing_period IN ('MONTHLY', 'YEARLY')),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    agreed_price NUMERIC(12, 2) NOT NULL CHECK (agreed_price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_package_product
        FOREIGN KEY (package_id, product) REFERENCES packages(id, product) ON DELETE RESTRICT,
    CONSTRAINT ck_subscription_dates CHECK (ends_at IS NULL OR ends_at > starts_at)
);

CREATE UNIQUE INDEX uk_subscriptions_one_active_per_product
    ON subscriptions(organization_id, product)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_subscriptions_org_product_created
    ON subscriptions(organization_id, product, created_at DESC);
