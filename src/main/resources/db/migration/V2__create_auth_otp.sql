CREATE TABLE auth_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    phone VARCHAR(20) NOT NULL,

    otp_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    attempts INTEGER NOT NULL DEFAULT 0,

    verified BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    verified_at TIMESTAMPTZ
);

CREATE INDEX idx_auth_otps_phone
    ON auth_otps(phone);

CREATE INDEX idx_auth_otps_phone_created
    ON auth_otps(phone, created_at);