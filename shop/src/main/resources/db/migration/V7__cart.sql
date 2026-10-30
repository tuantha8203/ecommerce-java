CREATE SCHEMA IF NOT EXISTS cart;

-- The cart is simply the set of rows for one customer: no separate "carts" table needed
CREATE TABLE cart.cart_items (
    customer_id  bigint      NOT NULL,
    product_id   bigint      NOT NULL,
    quantity     int         NOT NULL CONSTRAINT ck_cart_items_quantity CHECK (quantity BETWEEN 1 AND 99),
    added_at     timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_cart_items PRIMARY KEY (customer_id, product_id)
);