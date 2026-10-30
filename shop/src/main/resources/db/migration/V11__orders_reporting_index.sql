-- Reports only read paid orders within a date range
CREATE INDEX idx_orders_paid_created
    ON ordering.orders (created_at)
    WHERE status IN ('PAID', 'SHIPPED', 'DELIVERED');