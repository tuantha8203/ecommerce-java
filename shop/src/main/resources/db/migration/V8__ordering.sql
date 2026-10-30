CREATE TABLE ordering.orders (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id         bigint       NOT NULL,
    status              varchar(20)  NOT NULL
                        CONSTRAINT ck_orders_status
                        CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total               bigint       NOT NULL CONSTRAINT ck_orders_total CHECK (total >= 0),
    -- shipping address copied at order time: later edits to the customer's addresses must not change it
    ship_receiver_name  varchar(150) NOT NULL,
    ship_phone          varchar(20)  NOT NULL,
    ship_street         varchar(255) NOT NULL,
    ship_ward           varchar(100) NOT NULL,
    ship_province       varchar(100) NOT NULL,
    cancel_reason       varchar(255),
    version             bigint       NOT NULL DEFAULT 0,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_customer_created ON ordering.orders (customer_id, created_at DESC, id DESC);

CREATE TABLE ordering.order_lines (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      bigint       NOT NULL REFERENCES ordering.orders (id) ON DELETE CASCADE,
    product_id    bigint       NOT NULL,   -- no foreign key to catalog: different module
    product_name  varchar(255) NOT NULL,   -- copied at order time
    unit_price    bigint       NOT NULL CONSTRAINT ck_order_lines_unit_price CHECK (unit_price >= 0),
    quantity      int          NOT NULL CONSTRAINT ck_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT uq_order_lines_product UNIQUE (order_id, product_id)
);

CREATE TABLE ordering.idempotency_keys (
    customer_id      bigint       NOT NULL,
    idempotency_key  varchar(100) NOT NULL,
    status           varchar(20)  NOT NULL
                     CONSTRAINT ck_idempotency_keys_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    order_id         bigint,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (customer_id, idempotency_key)
);