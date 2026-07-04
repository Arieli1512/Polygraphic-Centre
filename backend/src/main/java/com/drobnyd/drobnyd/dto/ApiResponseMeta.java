package com.drobnyd.drobnyd.dto;

import java.time.Instant;

public record ApiResponseMeta(
        String requestId,
        Instant timestamp) {
}