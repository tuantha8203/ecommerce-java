CREATE TABLE customer.customers (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       varchar(255) NOT NULL,
    full_name   varchar(150) NOT NULL,
    phone       varchar(20),
    version     bigint       NOT NULL DEFAULT 0,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

-- Email không phân biệt hoa thường: An@Mail.com và an@mail.com là một người
CREATE UNIQUE INDEX uq_customers_email_lower ON customer.customers (lower(email));

CREATE TABLE customer.addresses (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id    bigint       NOT NULL REFERENCES customer.customers (id) ON DELETE CASCADE,
    receiver_name  varchar(150) NOT NULL,
    phone          varchar(20)  NOT NULL,
    street         varchar(255) NOT NULL,   -- số nhà, tên đường
    ward           varchar(100) NOT NULL,   -- phường/xã
    province       varchar(100) NOT NULL,   -- tỉnh/thành phố
    is_default     boolean      NOT NULL DEFAULT false,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_customer ON customer.addresses (customer_id);

-- Mỗi khách tối đa MỘT địa chỉ mặc định
CREATE UNIQUE INDEX uq_addresses_one_default
    ON customer.addresses (customer_id)
    WHERE is_default;