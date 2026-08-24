-- Communication module (PRD §8.4): notifications + chat messages.

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    -- EMAIL | SMS | PUSH
    channel VARCHAR(16) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    template VARCHAR(128),
    subject VARCHAR(512),
    body TEXT,
    -- PENDING | SENT | FAILED | RATE_LIMITED
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(512),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications (recipient);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications (status);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    -- Convention: "order:{orderId}" for order-scoped rooms
    room_id VARCHAR(255) NOT NULL,
    sender_id UUID NOT NULL,
    sender_role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_room_time ON chat_messages (room_id, created_at);
