package com.drobnyd.drobnyd.exception;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ApiProblemFactory {

    public ApiProblemDetails create(
            HttpStatus status,
            String type,
            String title,
            String detail,
            String code,
            String userMessage,
            String action,
            HttpServletRequest request,
            boolean retryable,
            @Nullable List<ApiValidationIssue> errors,
            @Nullable String docs) {
        return new ApiProblemDetails(
                type,
                title,
                status.value(),
                detail,
                request.getRequestURI(),
                code,
                userMessage,
                action,
                requestId(),
                traceId(),
                Instant.now(),
                errors,
                retryable,
                docs);
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    private @Nullable String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? null : traceId;
    }
}