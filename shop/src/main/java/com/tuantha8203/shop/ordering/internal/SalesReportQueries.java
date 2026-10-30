package com.tuantha8203.shop.ordering.internal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class SalesReportQueries {

    private final JdbcClient jdbc;

    List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to, ZoneId zone) {
        return jdbc.sql("""
                        WITH days AS (
                            -- every day of the range, so days without sales still show up as 0
                            SELECT CAST(d AS date) AS day
                            FROM generate_series(CAST(:from AS date), CAST(:to AS date), interval '1 day') AS d
                        ),
                        sales AS (
                            SELECT CAST(o.created_at AT TIME ZONE :zone AS date) AS day,
                                   count(*)                                       AS orders,
                                   sum(o.total)                                   AS revenue
                            FROM ordering.orders o
                            WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
                              -- filter on the raw column (not on an expression), so the index can be used
                              AND o.created_at >= :start AND o.created_at < :end
                            GROUP BY 1
                        )
                        SELECT days.day, coalesce(sales.orders, 0) AS orders, coalesce(sales.revenue, 0) AS revenue
                        FROM days LEFT JOIN sales USING (day)
                        ORDER BY days.day
                        """)
                .param("from", from)
                .param("to", to)
                .param("zone", zone.getId())
                .param("start", startOf(from, zone))
                .param("end", startOf(to.plusDays(1), zone))
                .query((rs, rowNum) -> new DailyRevenue(
                        rs.getObject("day", LocalDate.class), rs.getLong("orders"), rs.getLong("revenue")))
                .list();
    }

    List<TopProduct> topProducts(LocalDate from, LocalDate to, ZoneId zone, int limit) {
        return jdbc.sql("""
                        SELECT l.product_id,
                               -- a product may have been renamed: show the name from its most recent order
                               (array_agg(l.product_name ORDER BY o.created_at DESC))[1] AS product_name,
                               sum(l.quantity)                                          AS units_sold,
                               sum(l.unit_price * l.quantity)                           AS revenue
                        FROM ordering.order_lines l
                        JOIN ordering.orders o ON o.id = l.order_id
                        WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
                          AND o.created_at >= :start AND o.created_at < :end
                        GROUP BY l.product_id
                        ORDER BY units_sold DESC, revenue DESC
                        LIMIT :limit
                        """)
                .param("start", startOf(from, zone))
                .param("end", startOf(to.plusDays(1), zone))
                .param("limit", limit)
                .query((rs, rowNum) -> new TopProduct(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getLong("units_sold"),
                        rs.getLong("revenue")))
                .list();
    }

    /** Midnight of that day in the shop's time zone, as an absolute instant. */
    private static OffsetDateTime startOf(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone).toInstant().atOffset(ZoneOffset.UTC);
    }
}
