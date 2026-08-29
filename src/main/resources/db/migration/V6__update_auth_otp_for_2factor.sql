-- =====================================================
-- V6
-- Replace locally generated OTP hash with 2Factor
-- provider session ID
-- =====================================================

ALTER TABLE auth_otps
    DROP COLUMN otp_hash;

ALTER TABLE auth_otps
    ADD COLUMN provider_session_id VARCHAR(200);

-- Existing OTP records are no longer valid for 2Factor.
-- Give them a temporary provider session value so the
-- NOT NULL constraint can be safely applied.
UPDATE auth_otps
SET provider_session_id = 'LEGACY-' || id::text
WHERE provider_session_id IS NULL;

ALTER TABLE auth_otps
    ALTER COLUMN provider_session_id SET NOT NULL;