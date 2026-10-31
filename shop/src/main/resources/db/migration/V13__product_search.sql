CREATE EXTENSION IF NOT EXISTS unaccent SCHEMA public;

-- unaccent() is only STABLE (its dictionary could change), so PostgreSQL refuses it inside a
-- generated column or an index. This wrapper promises the same input always gives the same output,
-- which is true as long as nobody edits the unaccent rules file.
CREATE OR REPLACE FUNCTION catalog.immutable_unaccent(text)
    RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

-- Kept up to date by PostgreSQL itself on every INSERT/UPDATE: application code never writes it.
-- Weight A (name) counts more than weight B (description) when ranking.
-- 'simple' = lowercase words, no stemming: PostgreSQL has no Vietnamese dictionary.
ALTER TABLE catalog.products
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', catalog.immutable_unaccent(coalesce(name, ''))), 'A') ||
        setweight(to_tsvector('simple', catalog.immutable_unaccent(coalesce(description, ''))), 'B')
    ) STORED;

-- GIN = an inverted index (word → rows). Partial: only products customers can find.
CREATE INDEX idx_products_search ON catalog.products USING gin (search_vector) WHERE status = 'ACTIVE';