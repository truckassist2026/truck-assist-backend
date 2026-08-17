-- =========================================================
-- TRUCK ASSIST
-- V4 - MECHANIC LOCATION
-- =========================================================

ALTER TABLE mechanics
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7);

ALTER TABLE mechanics
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);

ALTER TABLE mechanics
    ADD COLUMN IF NOT EXISTS last_location_at TIMESTAMPTZ;

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_mechanics_available
    ON mechanics(is_available);

CREATE INDEX IF NOT EXISTS idx_mechanics_location
    ON mechanics(latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_mechanics_location_updated
    ON mechanics(last_location_at);