package com.drobnyd.drobnyd.dto;

import com.drobnyd.drobnyd.dto.ApiFieldError;

import java.time.Instant;
import java.util.List;

public record ApiProblemResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    String code,
    String userMessage,
    String action,
    List<ApiFieldError> fieldErrors,
    boolean retryable,
    String requestId,
    String traceId,
    Instant timestamp) {
}