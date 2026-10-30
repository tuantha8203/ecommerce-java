package com.tuantha8203.shop.ordering.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuantha8203.shop.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SalesReportIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    private long order(String status, String createdAtUtc, long total) {
        return jdbc.sql("""
                        INSERT INTO ordering.orders
                            (customer_id, status, total, ship_receiver_name, ship_phone, ship_street,
                             ship_ward, ship_province, created_at, updated_at)
                        VALUES (999, :status, :total, 'R', '0900000000', 'S', 'W', 'P', CAST(:createdAt AS timestamptz), now())
                        RETURNING id
                        """)
                .param("status", status)
                .param("total", total)
                .param("createdAt", createdAtUtc)
                .query(Long.class)
                .single();
    }

    private void line(long orderId, long productId, String name, long unitPrice, int quantity) {
        jdbc.sql("""
                        INSERT INTO ordering.order_lines (order_id, product_id, product_name, unit_price, quantity)
                        VALUES (:orderId, :productId, :name, :unitPrice, :quantity)
                        """)
                .param("orderId", orderId)
                .param("productId", productId)
                .param("name", name)
                .param("unitPrice", unitPrice)
                .param("quantity", quantity)
                .update();
    }

    @Test
    void ordersAreCountedOnTheirVietnameseCalendarDay() throws Exception {
        // 16:59:59 UTC = 23:59:59 in Vietnam, still January 1st
        order("PAID", "2020-01-01T16:59:59Z", 100_000);
        // 17:00:00 UTC = 00:00:00 in Vietnam, already January 2nd
        order("PAID", "2020-01-01T17:00:00Z", 200_000);

        mvc.perform(get("/api/v1/admin/reports/daily-revenue")
                        .param("from", "2020-01-01")
                        .param("to", "2020-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].day").value("2020-01-01"))
                .andExpect(jsonPath("$.days[0].revenue").value(100_000))
                .andExpect(jsonPath("$.days[1].day").value("2020-01-02"))
                .andExpect(jsonPath("$.days[1].revenue").value(200_000));
    }

    @Test
    void onlyPaidOrdersCountAndEmptyDaysShowZero() throws Exception {
        order("PAID", "2020-02-10T03:00:00Z", 100_000);
        order("DELIVERED", "2020-02-10T04:00:00Z", 50_000);
        order("CANCELLED", "2020-02-10T05:00:00Z", 999_000);
        order("PENDING_PAYMENT", "2020-02-10T06:00:00Z", 999_000);

        mvc.perform(get("/api/v1/admin/reports/daily-revenue")
                        .param("from", "2020-02-09")
                        .param("to", "2020-02-11"))
                .andExpect(jsonPath("$.days.length()").value(3))
                .andExpect(jsonPath("$.days[0].revenue").value(0)) // Feb 9: no sales, still listed
                .andExpect(jsonPath("$.days[1].orders").value(2))
                .andExpect(jsonPath("$.days[1].revenue").value(150_000))
                .andExpect(jsonPath("$.totalRevenue").value(150_000));
    }

    @Test
    void topProductsAreRankedByUnitsAndShowTheLatestName() throws Exception {
        long first = order("PAID", "2020-03-05T03:00:00Z", 0);
        line(first, 1, "Old shirt name", 100_000, 2);
        line(first, 2, "Socks", 20_000, 1);
        long second = order("SHIPPED", "2020-03-06T03:00:00Z", 0);
        line(second, 1, "New shirt name", 100_000, 3);
        long cancelled = order("CANCELLED", "2020-03-06T04:00:00Z", 0);
        line(cancelled, 2, "Socks", 20_000, 100);

        mvc.perform(get("/api/v1/admin/reports/top-products")
                        .param("from", "2020-03-01")
                        .param("to", "2020-03-31"))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName").value("New shirt name"))
                .andExpect(jsonPath("$[0].unitsSold").value(5))
                .andExpect(jsonPath("$[1].productId").value(2))
                .andExpect(jsonPath("$[1].unitsSold").value(1)); // the cancelled 100 socks are ignored
    }

    @Test
    void invalidRangesAreRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/reports/daily-revenue")
                        .param("from", "2020-05-10")
                        .param("to", "2020-05-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/reports/daily-revenue")
                        .param("from", "2019-01-01")
                        .param("to", "2020-12-31"))
                .andExpect(status().isBadRequest());
    }
}
