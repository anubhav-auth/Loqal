-- Platform module (PRD §8.3): storefront profiles + audit logs.

CREATE TABLE IF NOT EXISTS merchant_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(512),
    support_phone VARCHAR(32),
    address_line VARCHAR(512),
    city VARCHAR(255),
    -- Payout config placeholder (Phase 3: payment provider routing)
    payout_config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    actor_email VARCHAR(255),
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(128),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs (actor_user_id);
