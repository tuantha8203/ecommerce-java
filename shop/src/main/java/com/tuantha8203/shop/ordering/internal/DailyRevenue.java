package com.tuantha8203.shop.ordering.internal;

import java.time.LocalDate;

public record DailyRevenue(LocalDate day, long orders, long revenue) {}
