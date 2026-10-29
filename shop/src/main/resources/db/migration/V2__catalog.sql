CREATE TABLE catalog.categories (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id   bigint REFERENCES catalog.categories (id),
    name        varchar(100) NOT NULL,
    slug        varchar(120) NOT NULL UNIQUE,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_categories_parent ON catalog.categories (parent_id);

CREATE TABLE catalog.products (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id  bigint       NOT NULL REFERENCES catalog.categories (id),
    sku          varchar(64)  NOT NULL UNIQUE,
    name         varchar(255) NOT NULL,
    slug         varchar(255) NOT NULL UNIQUE,
    description  text,
    price        bigint       NOT NULL CHECK (price >= 0),
    status       varchar(20)  NOT NULL DEFAULT 'DRAFT'
                 CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    version      bigint       NOT NULL DEFAULT 0,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category_created ON catalog.products (category_id, created_at, id);

CREATE TABLE catalog.product_images (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  bigint       NOT NULL REFERENCES catalog.products (id) ON DELETE CASCADE,
    url         varchar(500) NOT NULL,
    alt_text    varchar(255),
    position    int          NOT NULL DEFAULT 0,
    UNIQUE (product_id, position)
);