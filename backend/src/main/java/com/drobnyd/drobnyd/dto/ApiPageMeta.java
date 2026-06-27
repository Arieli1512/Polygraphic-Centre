package com.drobnyd.drobnyd.dto;

import java.time.Instant;

public record ApiPageMeta(
        String requestId,
        int page,
        int size,
        long totalItems,
        int totalPages,
        Instant timestamp
) {
}