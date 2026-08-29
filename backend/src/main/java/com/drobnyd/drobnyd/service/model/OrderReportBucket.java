package com.drobnyd.drobnyd.service.model;

public record OrderReportBucket(
        String period,
        int orderCount,
        long revenue) {
}
