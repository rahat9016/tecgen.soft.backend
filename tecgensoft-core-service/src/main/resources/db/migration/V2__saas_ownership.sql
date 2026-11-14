CREATE TABLE organizations(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 name varchar(255) NOT NULL,
 active boolean NOT NULL,
 admin_user_id uuid NOT NULL,
 admin_role_id uuid NOT NULL
);
CREATE TABLE product_packages(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 code varchar(255) NOT NULL,
 display_name varchar(255) NOT NULL,
 product varchar(255) NOT NULL,
 description text,
 currency varchar(255) NOT NULL,
 price numeric(38,2) NOT NULL,
 version integer NOT NULL,
 active boolean NOT NULL,
 available_for_sale boolean NOT NULL,
 features jsonb NOT NULL,
 limits jsonb NOT NULL
);
CREATE TABLE subscriptions(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 package_id uuid NOT NULL,
 product varchar(255) NOT NULL,
 status varchar(255) NOT NULL,
 billing_period varchar(255) NOT NULL,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz NOT NULL,
 agreed_price numeric(38,2) NOT NULL,
 currency varchar(255) NOT NULL
);
CREATE TABLE tenant_domains(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 product varchar(255) NOT NULL,
 hostname varchar(255) NOT NULL,
 challenge varchar(255) NOT NULL,
 verified_at timestamptz
);
CREATE TABLE admin_assignment_audit(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 user_id uuid NOT NULL,
 actor_id uuid NOT NULL
);
ALTER TABLE organizations ADD CONSTRAINT org_admin_fk FOREIGN KEY(admin_user_id) REFERENCES users(id), ADD CONSTRAINT org_role_fk FOREIGN KEY(admin_role_id) REFERENCES roles(id), ADD CONSTRAINT org_role_unique UNIQUE(admin_role_id);
ALTER TABLE branches ADD COLUMN organization_id uuid REFERENCES organizations(id);
CREATE INDEX idx_branch_organization ON branches(organization_id);
ALTER TABLE branches ADD CONSTRAINT branch_org_pair UNIQUE(organization_id,id);
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_scope_check;
ALTER TABLE roles ADD CONSTRAINT roles_scope_check CHECK(scope IN ('PLATFORM','BRANCH','ORGANIZATION'));
ALTER TABLE product_packages ADD CONSTRAINT package_code_version UNIQUE(code,version),ADD CHECK(product='TURF'),ADD CHECK(price>=0),ADD CHECK(version>0);
ALTER TABLE subscriptions ADD FOREIGN KEY(organization_id) REFERENCES organizations(id),ADD FOREIGN KEY(package_id) REFERENCES product_packages(id),ADD CHECK(ends_at>starts_at),ADD CHECK(agreed_price>=0);
CREATE UNIQUE INDEX one_active_product_subscription ON subscriptions(organization_id,product) WHERE status='ACTIVE';
ALTER TABLE tenant_domains ADD FOREIGN KEY(organization_id) REFERENCES organizations(id),ADD UNIQUE(hostname),ADD CHECK(product='TURF');
ALTER TABLE admin_assignment_audit ADD FOREIGN KEY(organization_id) REFERENCES organizations(id),ADD FOREIGN KEY(user_id) REFERENCES users(id),ADD FOREIGN KEY(actor_id) REFERENCES users(id);
CREATE INDEX subscription_org ON subscriptions(organization_id,product);
