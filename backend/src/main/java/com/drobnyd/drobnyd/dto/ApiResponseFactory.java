package com.drobnyd.drobnyd.dto;

import java.time.Instant;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    public <T> ApiSuccessResponse<T> success(T data) {
        return new ApiSuccessResponse<>(
                data,
                new ApiResponseMeta(requestId(), Instant.now()));
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }
}