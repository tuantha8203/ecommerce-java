package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.InvalidRequestException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
class AdminReportController {

    private static final long MAX_DAYS = 366;

    private final SalesReportQueries queries;
    private final ZoneId zone;

    AdminReportController(
            SalesReportQueries queries, @Value("${ecommerce-java.reporting.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.queries = queries;
        this.zone = ZoneId.of(zone);
    }

    @GetMapping("/daily-revenue")
    DailyRevenueReport dailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireValidRange(from, to);
        return DailyRevenueReport.of(queries.dailyRevenue(from, to, zone));
    }

    @GetMapping("/top-products")
    List<TopProduct> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        requireValidRange(from, to);
        return queries.topProducts(from, to, zone, limit);
    }

    /** An unbounded range would let one request scan the whole orders table. */
    private static void requireValidRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new InvalidRequestException("The range cannot exceed " + MAX_DAYS + " days");
        }
    }
}
