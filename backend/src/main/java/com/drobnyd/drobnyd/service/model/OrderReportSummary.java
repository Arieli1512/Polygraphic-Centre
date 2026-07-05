package com.drobnyd.drobnyd.service.model;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderReportSummary(
        OffsetDateTime from,
        OffsetDateTime to,
        String reportType,
        int totalOrders,
        long totalRevenue,
        long averageLeadTimeMinutes,
        List<OrderReportBucket> buckets) {
}
