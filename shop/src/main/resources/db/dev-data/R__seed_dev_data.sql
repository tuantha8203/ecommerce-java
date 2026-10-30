-- Repeatable migration (R__): Flyway re-runs it whenever its content changes,
-- so the guard below makes it safe to run more than once.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM catalog.products WHERE sku LIKE 'SEED-%') THEN

        INSERT INTO catalog.categories (name, slug)
        SELECT 'Category ' || g, 'seed-category-' || g
        FROM generate_series(1, 50) AS g;

        INSERT INTO catalog.products (category_id, sku, name, slug, description, price, status, created_at, updated_at)
        SELECT c.id,
               'SEED-' || g,
               'Product ' || g,
               'seed-product-' || g,
               'Seeded product number ' || g,
               (10 + floor(random() * 990)::int) * 1000,
               CASE WHEN random() < 0.9 THEN 'ACTIVE' ELSE 'DRAFT' END,
               now() - random() * interval '365 days',
               now()
        FROM generate_series(1, 200000) AS g
        JOIN catalog.categories c ON c.slug = 'seed-category-' || (1 + g % 50);

        ANALYZE catalog.products;   -- refresh statistics so the planner knows the table is big now
    END IF;
END $$;