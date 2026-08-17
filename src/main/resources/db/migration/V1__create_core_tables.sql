-- =====================================================
-- TRUCK ASSIST
-- V1 - CORE TABLES
-- =====================================================


-- =====================================================
-- USERS
-- =====================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    phone VARCHAR(20) NOT NULL UNIQUE,

    name VARCHAR(150),

    email VARCHAR(255),

    role VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    profile_image_url TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================
-- DRIVERS
-- =====================================================

CREATE TABLE drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL UNIQUE,

    license_number VARCHAR(100),

    license_expiry_date DATE,

    emergency_contact_name VARCHAR(150),

    emergency_contact_phone VARCHAR(20),

    is_available BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_driver_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


-- =====================================================
-- MECHANICS
-- =====================================================

CREATE TABLE mechanics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL UNIQUE,

    experience_years INTEGER,

    workshop_name VARCHAR(200),

    workshop_address TEXT,

    is_available BOOLEAN NOT NULL DEFAULT FALSE,

    rating NUMERIC(3,2) NOT NULL DEFAULT 0,

    total_jobs INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mechanic_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


-- =====================================================
-- VEHICLES
-- =====================================================

CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    driver_id UUID NOT NULL,

    registration_number VARCHAR(30) NOT NULL UNIQUE,

    manufacturer VARCHAR(100),

    model VARCHAR(100),

    vehicle_type VARCHAR(100),

    manufacturing_year INTEGER,

    color VARCHAR(50),

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vehicle_driver
        FOREIGN KEY (driver_id)
        REFERENCES drivers(id)
        ON DELETE CASCADE
);


-- =====================================================
-- SERVICE CATEGORIES
-- =====================================================

CREATE TABLE service_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(100) NOT NULL,

    description TEXT,

    icon VARCHAR(100),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    display_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================
-- MECHANIC SERVICES
-- =====================================================

CREATE TABLE mechanic_services (
    mechanic_id UUID NOT NULL,

    service_category_id UUID NOT NULL,

    PRIMARY KEY (mechanic_id, service_category_id),

    CONSTRAINT fk_mechanic_service_mechanic
        FOREIGN KEY (mechanic_id)
        REFERENCES mechanics(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mechanic_service_category
        FOREIGN KEY (service_category_id)
        REFERENCES service_categories(id)
        ON DELETE CASCADE
);


-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_users_role
    ON users(role);

CREATE INDEX idx_users_status
    ON users(status);

CREATE INDEX idx_drivers_available
    ON drivers(is_available);

CREATE INDEX idx_mechanics_available
    ON mechanics(is_available);

CREATE INDEX idx_vehicles_driver
    ON vehicles(driver_id);

CREATE INDEX idx_service_categories_active
    ON service_categories(is_active);


-- =====================================================
-- INITIAL SERVICE CATEGORIES
-- =====================================================

INSERT INTO service_categories
    (code, name, description, icon, display_order)
VALUES
    (
        'BATTERY',
        'Battery',
        'Battery jump start or replacement assistance',
        'battery-half-outline',
        1
    ),
    (
        'TYRE',
        'Tyre',
        'Tyre puncture or tyre replacement assistance',
        'speedometer-outline',
        2
    ),
    (
        'FUEL',
        'Fuel',
        'Fuel delivery assistance',
        'flame-outline',
        3
    ),
    (
        'MECHANICAL',
        'Mechanical',
        'General mechanical breakdown assistance',
        'construct-outline',
        4
    ),
    (
        'ELECTRICAL',
        'Electrical',
        'Vehicle electrical issue assistance',
        'flash-outline',
        5
    ),
    (
        'TOWING',
        'Towing',
        'Vehicle towing assistance',
        'car-outline',
        6
    );