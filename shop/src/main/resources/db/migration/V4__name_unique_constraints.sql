ALTER TABLE catalog.categories     RENAME CONSTRAINT categories_slug_key                    TO uq_categories_slug;
ALTER TABLE catalog.products       RENAME CONSTRAINT products_sku_key                       TO uq_products_sku;
ALTER TABLE catalog.products       RENAME CONSTRAINT products_slug_key                      TO uq_products_slug;
ALTER TABLE catalog.product_images RENAME CONSTRAINT product_images_product_id_position_key TO uq_product_images_position;