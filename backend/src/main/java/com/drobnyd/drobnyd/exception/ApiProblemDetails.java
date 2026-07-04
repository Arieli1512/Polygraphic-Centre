package com.drobnyd.drobnyd.exception;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProblemDetails", description = "Standard RFC 7807 problem details response with domain diagnostics.")
public record ApiProblemDetails(
        @Schema(example = "https://api.polygraphic-centre.dev/problems/validation-error") String type,
        @Schema(example = "Validation Error") String title,
        @Schema(example = "422") int status,
        @Schema(example = "Validation failed for request body.") String detail,
        @Schema(example = "/api/auth/session") String instance,
        @Schema(example = "VALIDATION_ERROR") String code,
        @Schema(example = "Nie udalo sie zapisac danych, bo czesc pol jest niepoprawna.") String userMessage,
        @Schema(example = "Popraw oznaczone pola i sprobuj ponownie.") String action,
        @Schema(example = "0f6b6ca8-95af-4e1f-9b0b-3e31f5f49f8e") String requestId,
        @Schema(example = "3e7e26f4d3f498f1", nullable = true) @Nullable String traceId,
        @Schema(example = "2026-04-14T11:44:00Z") Instant timestamp,
        @ArraySchema(schema = @Schema(implementation = ApiValidationIssue.class), arraySchema = @Schema(nullable = true)) @Nullable List<ApiValidationIssue> errors,
        @Schema(example = "false") boolean retryable,
        @Schema(nullable = true) @Nullable String docs) {
}