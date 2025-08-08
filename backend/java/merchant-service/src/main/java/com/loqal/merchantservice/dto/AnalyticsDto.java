package com.loqal.merchantservice.dto;

import java.util.List;

public record AnalyticsDto(double totalRevenue, int ordersToday, List<ProductDto> topSellingProducts) {}