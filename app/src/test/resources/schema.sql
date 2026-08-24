CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    category_name VARCHAR(255),
    category_description VARCHAR(255),
    price DOUBLE PRECISION,
    quantity INT,
    image_urls TEXT[],
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    merchant_id UUID
);

CREATE TABLE IF NOT EXISTS processed_events (
    order_id UUID PRIMARY KEY,
    status VARCHAR(64),
    reason VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    customer_id UUID,
    merchant_id UUID,
    delivery_agent_id UUID,
    total_amount DOUBLE PRECISION,
    discount_amount DOUBLE PRECISION,
    final_amount DOUBLE PRECISION,
    payment_status VARCHAR(64),
    delivery_address_id UUID,
    current_status VARCHAR(64),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS order_item (
    id UUID PRIMARY KEY,
    order_id UUID,
    product_id UUID,
    quantity INT,
    price_at_purchase DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64),
    event_type VARCHAR(64),
    payload TEXT,
    destination_topic VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    order_id UUID,
    user_id UUID,
    razorpay_payment_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    amount_minor BIGINT,
    currency VARCHAR(8),
    status VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    payment_id UUID,
    razorpay_refund_id VARCHAR(255),
    amount_minor BIGINT,
    status VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

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
