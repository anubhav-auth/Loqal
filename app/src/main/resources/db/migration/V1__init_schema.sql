-- Loqal canonical schema (PRD §10).
-- Money columns are BIGINT minor units (paise) — PRD §9.2.

-- ============ identity ============
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255),
    full_name VARCHAR(255),
    phone_number VARCHAR(32),
    profile_picture_url VARCHAR(512),
    roles TEXT[],
    street VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    postal_code VARCHAR(16),
    country VARCHAR(255),
    tenant_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id UUID PRIMARY KEY,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    token TEXT,
    email VARCHAR(255),
    expiration TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    type VARCHAR(32),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_email ON refresh_tokens (email);

-- ============ catalog ============
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    category_name VARCHAR(255),
    category_description VARCHAR(255),
    price_minor BIGINT NOT NULL DEFAULT 0,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    image_urls TEXT[],
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    merchant_id UUID
);

CREATE INDEX IF NOT EXISTS idx_products_merchant_active ON products (merchant_id, quantity);

CREATE TABLE IF NOT EXISTS processed_events (
    order_id UUID PRIMARY KEY,
    status VARCHAR(64),
    reason VARCHAR(255)
);

-- ============ orders ============
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    customer_id UUID,
    merchant_id UUID,
    delivery_agent_id UUID,
    total_amount_minor BIGINT NOT NULL DEFAULT 0,
    discount_amount_minor BIGINT NOT NULL DEFAULT 0,
    final_amount_minor BIGINT NOT NULL DEFAULT 0,
    payment_status VARCHAR(64),
    delivery_address_id UUID,
    current_status VARCHAR(64),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_merchant ON orders (merchant_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (current_status);

CREATE TABLE IF NOT EXISTS order_item (
    id UUID PRIMARY KEY,
    order_id UUID,
    product_id UUID,
    quantity INT,
    price_at_purchase_minor BIGINT
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_item (order_id);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64),
    event_type VARCHAR(64),
    payload TEXT,
    destination_topic VARCHAR(255),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_created ON outbox_events (created_at);

-- ============ payments ============
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    order_id UUID,
    user_id UUID,
    razorpay_payment_id VARCHAR(255) UNIQUE,
    razorpay_order_id VARCHAR(255) UNIQUE,
    amount_minor BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(8),
    status VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    payment_id UUID,
    razorpay_refund_id VARCHAR(255) UNIQUE,
    amount_minor BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
