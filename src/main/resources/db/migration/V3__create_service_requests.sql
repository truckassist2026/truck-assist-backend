-- =========================================================
-- TRUCK ASSIST
-- V3 - SERVICE REQUESTS
-- =========================================================

CREATE TABLE service_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    driver_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,

    category VARCHAR(30) NOT NULL,
    description VARCHAR(1000),

    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    address VARCHAR(500),

    status VARCHAR(30) NOT NULL DEFAULT 'SEARCHING',

    assigned_mechanic_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    CONSTRAINT fk_service_request_driver
        FOREIGN KEY (driver_id)
        REFERENCES drivers(id),

    CONSTRAINT fk_service_request_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id),

    CONSTRAINT fk_service_request_mechanic
        FOREIGN KEY (assigned_mechanic_id)
        REFERENCES mechanics(id),

    CONSTRAINT chk_service_request_category
        CHECK (
            category IN (
                'BREAKDOWN',
                'TYRE',
                'BATTERY',
                'FUEL',
                'OTHER'
            )
        ),

    CONSTRAINT chk_service_request_status
        CHECK (
            status IN (
                'CREATED',
                'SEARCHING',
                'ASSIGNED',
                'MECHANIC_EN_ROUTE',
                'ARRIVED',
                'IN_PROGRESS',
                'COMPLETED',
                'PAYMENT_PENDING',
                'PAID',
                'RATED',
                'CANCELLED'
            )
        )
);

-- =========================================================
-- STATUS HISTORY
-- =========================================================

CREATE TABLE request_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    request_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    changed_by_user_id UUID,

    notes VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_request_history_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_request_history_user
        FOREIGN KEY (changed_by_user_id)
        REFERENCES users(id)
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_service_requests_driver
    ON service_requests(driver_id);

CREATE INDEX idx_service_requests_vehicle
    ON service_requests(vehicle_id);

CREATE INDEX idx_service_requests_status
    ON service_requests(status);

CREATE INDEX idx_service_requests_mechanic
    ON service_requests(assigned_mechanic_id);

CREATE INDEX idx_service_requests_created_at
    ON service_requests(created_at DESC);

CREATE INDEX idx_request_history_request
    ON request_status_history(request_id);

CREATE INDEX idx_request_history_created_at
    ON request_status_history(created_at DESC);