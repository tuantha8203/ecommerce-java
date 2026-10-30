CREATE TABLE payment.payments (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_ref          varchar(64)  NOT NULL CONSTRAINT uq_payments_order_ref UNIQUE,
    amount             bigint       NOT NULL CONSTRAINT ck_payments_amount CHECK (amount > 0),
    status             varchar(20)  NOT NULL
                       CONSTRAINT ck_payments_status CHECK (status IN
                           ('PENDING', 'UNKNOWN', 'SUCCEEDED', 'DECLINED', 'REFUND_REQUESTED', 'REFUNDED')),
    idempotency_key    varchar(100) NOT NULL,
    payment_token      varchar(100),
    gateway_charge_id  varchar(100),
    failure_reason     varchar(255),
    version            bigint       NOT NULL DEFAULT 0,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now()
);

-- Work queues for the background jobs: small partial indexes over the few rows that need attention
CREATE INDEX idx_payments_unresolved ON payment.payments (updated_at) WHERE status IN ('PENDING', 'UNKNOWN');
CREATE INDEX idx_payments_refund_requested ON payment.payments (updated_at) WHERE status = 'REFUND_REQUESTED';

-- Every webhook event id we have applied: a duplicate delivery is recognized and ignored
CREATE TABLE payment.processed_webhook_events (
    event_id     varchar(100) PRIMARY KEY,
    received_at  timestamptz  NOT NULL DEFAULT now()
);