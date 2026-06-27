package com.drobnyd.drobnyd.dto;

import java.time.Instant;

public record ApiProblemResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action,
        boolean retryable,
        String requestId,
        String traceId,
        Instant timestamp
) {
}