-- Public /venue and /content are gated on these features, but V5 never added them to TURF_BASIC.
INSERT INTO features(id, product_code, code, name, feature_type) VALUES
    ('00000000-0000-7000-8000-000000000114', 'TURF', 'VENUE_SETTINGS', 'Venue Settings', 'BOOLEAN'),
    ('00000000-0000-7000-8000-000000000115', 'TURF', 'WEBSITE_CONTENT', 'Website Content', 'BOOLEAN')
ON CONFLICT (product_code, code) DO NOTHING;

INSERT INTO package_features(id, package_id, feature_id, feature_value)
SELECT gen_random_uuid(), p.id, f.id, 'true'::jsonb
FROM packages p
JOIN features f ON f.product_code = 'TURF' AND f.code IN ('VENUE_SETTINGS', 'WEBSITE_CONTENT')
WHERE p.code = 'TURF_BASIC'
ON CONFLICT (package_id, feature_id) DO NOTHING;

UPDATE packages
SET features = features || '["VENUE_SETTINGS"]'::jsonb, updated_at = CURRENT_TIMESTAMP
WHERE code = 'TURF_BASIC' AND NOT features ? 'VENUE_SETTINGS';

UPDATE packages
SET features = features || '["WEBSITE_CONTENT"]'::jsonb, updated_at = CURRENT_TIMESTAMP
WHERE code = 'TURF_BASIC' AND NOT features ? 'WEBSITE_CONTENT';
