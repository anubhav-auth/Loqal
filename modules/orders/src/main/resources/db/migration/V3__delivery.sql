-- Delivery & fulfillment (PRD §8.2).

CREATE TABLE IF NOT EXISTS delivery_agents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(32),
    vehicle_type VARCHAR(32),
    -- OFF_DUTY | AVAILABLE | ON_DELIVERY
    status VARCHAR(16) NOT NULL DEFAULT 'OFF_DUTY',
    current_lat DOUBLE PRECISION,
    current_lng DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agents_tenant_status ON delivery_agents (tenant_id, status);

CREATE TABLE IF NOT EXISTS deliveries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL UNIQUE,
    agent_id UUID,
    -- ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED | FAILED
    status VARCHAR(16) NOT NULL DEFAULT 'ASSIGNED',
    pickup_otp VARCHAR(128),
    delivered_otp VARCHAR(128),
    assigned_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_deliveries_agent ON deliveries (agent_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_tenant_status ON deliveries (tenant_id, status);
