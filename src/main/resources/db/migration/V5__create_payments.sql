CREATE TABLE payments (

    id UUID PRIMARY KEY,

    service_request_id UUID NOT NULL,

    amount NUMERIC(12,2) NOT NULL,

    status VARCHAR(30) NOT NULL,

    payment_method VARCHAR(30),

    notes VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL,

    paid_at TIMESTAMPTZ,

    CONSTRAINT uk_payments_service_request
        UNIQUE (service_request_id),

    CONSTRAINT fk_payments_service_request
        FOREIGN KEY (service_request_id)
        REFERENCES service_requests(id)
);