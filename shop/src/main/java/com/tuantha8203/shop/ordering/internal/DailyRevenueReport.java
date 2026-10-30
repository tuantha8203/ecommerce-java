package com.tuantha8203.shop.ordering.internal;

import java.util.List;

public record DailyRevenueReport(List<DailyRevenue> days, long totalOrders, long totalRevenue) {

    static DailyRevenueReport of(List<DailyRevenue> days) {
        return new DailyRevenueReport(
                days,
                days.stream().mapToLong(DailyRevenue::orders).sum(),
                days.stream().mapToLong(DailyRevenue::revenue).sum());
    }
}
