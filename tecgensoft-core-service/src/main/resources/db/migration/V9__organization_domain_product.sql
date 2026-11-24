-- A hostname belongs to exactly one product site. Every domain registered so far is a turf site.
ALTER TABLE organization_domains
    ADD COLUMN product VARCHAR(40) NOT NULL DEFAULT 'TURF' REFERENCES products(code) ON DELETE RESTRICT;
