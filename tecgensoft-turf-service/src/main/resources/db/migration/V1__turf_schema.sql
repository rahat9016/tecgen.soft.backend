CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE venues(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 name varchar(255) NOT NULL,
 phone varchar(255) NOT NULL,
 address varchar(255) NOT NULL,
 timezone varchar(255) NOT NULL,
 opening_hour integer NOT NULL,
 closing_hour integer NOT NULL,
 peak_start_hour integer NOT NULL,
 advance_percentage integer NOT NULL,
 hold_minutes integer NOT NULL,
 max_duration_hours integer NOT NULL,
 booking_window_days integer NOT NULL,
 cancellation_cutoff_hours integer NOT NULL
);
CREATE TABLE courts(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 name varchar(255) NOT NULL,
 sport varchar(255) NOT NULL,
 surface varchar(255) NOT NULL,
 indoor boolean NOT NULL,
 size varchar(255),
 image_url varchar(255),
 regular_price numeric(19,2) NOT NULL,
 peak_price numeric(19,2) NOT NULL,
 players integer NOT NULL,
 features jsonb NOT NULL,
 description text,
 active boolean NOT NULL
);
CREATE TABLE reservations(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 court_id uuid NOT NULL,
 start_at timestamptz NOT NULL,
 end_at timestamptz NOT NULL,
 kind varchar(255) NOT NULL,
 status varchar(255) NOT NULL,
 expires_at timestamptz,
 secret_hash varchar(255),
 reason text
);
CREATE TABLE bookings(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 court_id uuid NOT NULL,
 reservation_id uuid NOT NULL,
 local_date date NOT NULL,
 start_hour integer NOT NULL,
 duration_hours integer NOT NULL,
 customer_name varchar(255) NOT NULL,
 customer_phone varchar(255) NOT NULL,
 customer_email varchar(255),
 total numeric(19,2) NOT NULL,
 required_advance numeric(19,2) NOT NULL,
 status varchar(255) NOT NULL,
 source varchar(255) NOT NULL,
 notes text,
 cancellation_reason text,
 cancelled_at timestamptz,
 cancelled_by uuid,
 submitted_reference varchar(255)
);
CREATE TABLE payments(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 booking_id uuid NOT NULL,
 amount numeric(19,2) NOT NULL,
 method varchar(255) NOT NULL,
 kind varchar(255) NOT NULL,
 reference varchar(255),
 idempotency_key varchar(255) NOT NULL,
 recorded_by uuid NOT NULL
);
CREATE TABLE contents(
 id uuid PRIMARY KEY,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL,
 branch_id uuid NOT NULL,
 kind varchar(255) NOT NULL,
 title varchar(255) NOT NULL,
 body text,
 image_url varchar(255),
 sort_order integer NOT NULL,
 active boolean NOT NULL
);
ALTER TABLE venues ADD UNIQUE(organization_id,branch_id),ADD UNIQUE(branch_id),ADD CHECK(opening_hour>=0 AND closing_hour<=24 AND closing_hour>opening_hour),ADD CHECK(advance_percentage BETWEEN 0 AND 100),ADD CHECK(hold_minutes BETWEEN 1 AND 60);
ALTER TABLE courts ADD UNIQUE(organization_id,branch_id,id),ADD FOREIGN KEY(organization_id,branch_id) REFERENCES venues(organization_id,branch_id),ADD CHECK(regular_price>=0 AND peak_price>=0),ADD CHECK(players>0);
ALTER TABLE reservations ADD UNIQUE(organization_id,branch_id,court_id,id),ADD FOREIGN KEY(organization_id,branch_id,court_id) REFERENCES courts(organization_id,branch_id,id),ADD CHECK(end_at>start_at),ADD CHECK(kind IN ('HOLD','BOOKING','BLOCK')),ADD CHECK(status IN ('ACTIVE','RELEASED','EXPIRED'));
ALTER TABLE reservations ADD CONSTRAINT no_occupancy_overlap EXCLUDE USING gist (court_id WITH =,tstzrange(start_at,end_at,'[)') WITH &&) WHERE(status='ACTIVE');
CREATE INDEX reservation_expiry ON reservations(expires_at) WHERE status='ACTIVE';
ALTER TABLE bookings ADD UNIQUE(organization_id,branch_id,id),ADD UNIQUE(reservation_id),ADD FOREIGN KEY(organization_id,branch_id,court_id,reservation_id) REFERENCES reservations(organization_id,branch_id,court_id,id),ADD CHECK(total>=0 AND required_advance>=0 AND required_advance<=total),ADD CHECK(start_hour>=0 AND duration_hours>0 AND start_hour+duration_hours<=24),ADD CHECK(status IN ('PENDING_PAYMENT','CONFIRMED','CANCELLED','EXPIRED')),ADD CHECK(source IN ('ONLINE','PHONE','WALK_IN'));
CREATE INDEX booking_tenant_date ON bookings(organization_id,branch_id,local_date);
ALTER TABLE payments ADD FOREIGN KEY(organization_id,branch_id,booking_id) REFERENCES bookings(organization_id,branch_id,id),ADD UNIQUE(organization_id,idempotency_key),ADD CHECK(amount>0),ADD CHECK(method IN ('BKASH','NAGAD','CARD','CASH')),ADD CHECK(kind IN ('ADVANCE','BALANCE','REFUND'));
CREATE UNIQUE INDEX unique_verified_transaction ON payments(method,reference) WHERE reference IS NOT NULL;
CREATE INDEX payment_booking ON payments(organization_id,branch_id,booking_id);
ALTER TABLE contents ADD FOREIGN KEY(organization_id,branch_id) REFERENCES venues(organization_id,branch_id),ADD CHECK(kind IN ('HERO','ABOUT','GALLERY','SERVICE'));
CREATE INDEX content_tenant ON contents(organization_id,branch_id,kind);
