CREATE TABLE stock (
    product_id UUID PRIMARY KEY,
    available_qty INTEGER NOT NULL,
    reserved_qty INTEGER NOT NULL DEFAULT 0,
    last_fencing_token BIGINT NOT NULL DEFAULT 0
);