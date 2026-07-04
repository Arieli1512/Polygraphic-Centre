package com.drobnyd.drobnyd.exception;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationProblemDetails", description = "Problem details response extended with field-level validation issues.")
public record ApiValidationProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action,
        String requestId,
        @Nullable String traceId,
        Instant timestamp,
        @ArraySchema(schema = @Schema(implementation = ApiValidationIssue.class)) List<ApiValidationIssue> errors,
        boolean retryable,
        @Nullable String docs) {
}