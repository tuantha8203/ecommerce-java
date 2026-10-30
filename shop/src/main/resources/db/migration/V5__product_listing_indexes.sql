-- Public feed: only ACTIVE products, newest first. Partial indexes stay small: drafts are not in them.
CREATE INDEX idx_products_published_feed
    ON catalog.products (created_at DESC, id DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_products_published_by_category
    ON catalog.products (category_id, created_at DESC, id DESC)
    WHERE status = 'ACTIVE';

-- Replaced by the partial index above; unused indexes still slow down every write
DROP INDEX catalog.idx_products_category_created;