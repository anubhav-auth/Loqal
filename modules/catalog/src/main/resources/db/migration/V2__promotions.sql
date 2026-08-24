-- Promotions module (PRD §8.1): coupons + redemption tracking.

CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    -- PERCENT: value = basis points (1000 = 10%); FIXED: value_minor off the subtotal
    discount_type VARCHAR(16) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    value BIGINT NOT NULL CHECK (value > 0),
    min_order_value_minor BIGINT NOT NULL DEFAULT 0,
    max_discount_minor BIGINT,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP,
    usage_limit_global INT,
    usage_limit_per_user INT,
    times_used INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

-- code unique per tenant
CREATE UNIQUE INDEX IF NOT EXISTS uq_coupons_tenant_code ON coupons (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_coupons_tenant_active ON coupons (tenant_id, active);

CREATE TABLE IF NOT EXISTS coupon_redemptions (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL REFERENCES coupons (id),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    discount_minor BIGINT NOT NULL,
    redeemed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_redemptions_coupon_user ON coupon_redemptions (coupon_id, user_id);
