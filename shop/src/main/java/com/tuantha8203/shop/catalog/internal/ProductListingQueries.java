package com.tuantha8203.shop.catalog.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductListingQueries {

    /** One row of the listing plus its created_at, which the next cursor needs. */
    record Row(ProductCard card, ProductCursor position) {}

    private final JdbcClient jdbc;

    List<Row> findPublished(String categorySlug, ProductCursor after, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.name, p.slug, p.price, p.created_at,
                       c.name AS category_name, i.url AS thumbnail_url
                FROM catalog.products p
                JOIN catalog.categories c ON c.id = p.category_id
                LEFT JOIN catalog.product_images i ON i.product_id = p.id AND i.position = 0
                WHERE p.status = 'ACTIVE'
                """);
        // Conditions are appended only when used: "OR :x IS NULL" tricks would stop PostgreSQL from using the index
        if (categorySlug != null) {
            sql.append(" AND c.slug = :category");
        }
        if (after != null) {
            sql.append(" AND (p.created_at, p.id) < (:afterCreatedAt, :afterId)");
        }
        sql.append(" ORDER BY p.created_at DESC, p.id DESC LIMIT :limit");

        JdbcClient.StatementSpec statement = jdbc.sql(sql.toString()).param("limit", limit);
        if (categorySlug != null) {
            statement = statement.param("category", categorySlug);
        }
        if (after != null) {
            statement = statement
                    .param("afterCreatedAt", after.createdAt().atOffset(ZoneOffset.UTC))
                    .param("afterId", after.id());
        }
        return statement.query(ProductListingQueries::mapRow).list();
    }

    private static Row mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductCard card = new ProductCard(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getLong("price"),
                rs.getString("category_name"),
                rs.getString("thumbnail_url"));
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        return new Row(card, new ProductCursor(createdAt.toInstant(), card.id()));
    }
}
