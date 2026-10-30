CREATE TABLE inventory.stock_items (
    product_id  bigint PRIMARY KEY,   -- refers to catalog.products, but no foreign key across modules
    on_hand     int    NOT NULL DEFAULT 0,
    reserved    int    NOT NULL DEFAULT 0,
    CONSTRAINT ck_stock_items_quantities CHECK (on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand)
);

CREATE TABLE inventory.stock_reservations (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_ref   varchar(64) NOT NULL,
    product_id  bigint      NOT NULL,
    quantity    int         NOT NULL CONSTRAINT ck_stock_reservations_quantity CHECK (quantity > 0),
    status      varchar(20) NOT NULL
                CONSTRAINT ck_stock_reservations_status CHECK (status IN ('ACTIVE', 'COMMITTED', 'RELEASED')),
    expires_at  timestamptz NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_stock_reservations_order_product UNIQUE (order_ref, product_id)
);

-- The expiry job only looks at ACTIVE reservations, ordered by expiry time
CREATE INDEX idx_stock_reservations_active_expiry
    ON inventory.stock_reservations (expires_at)
    WHERE status = 'ACTIVE';