package com.drobnyd.drobnyd.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "RFC Problem Details response extended with domain and diagnostic fields.")
public record ApiProblemResponse(
    @Schema(example = "https://api.polygraphic-centre.dev/problems/validation-error")
    String type,

    @Schema(example = "Validation Error")
    String title,

    @Schema(example = "422")
    int status,

    @Schema(example = "Validation failed for request body.")
    String detail,

    @Schema(example = "/api/v1/printing-points")
    String instance,

    @Schema(example = "VALIDATION_ERROR")
    String code,

    @Schema(example = "Nieprawidłowe dane formularza.")
    String userMessage,

    @Schema(example = "Popraw wskazane pola i spróbuj ponownie.")
    String action,

    List<ApiFieldError> errors,

    @Schema(example = "false")
    boolean retryable,

    @Schema(example = "0f6b6ca8-95af-4e1f-9b0b-3e31f5f49f8e")
    String requestId,

    @Schema(example = "3e7e26f4d3f498f1")
    String traceId,

    @Schema(example = "2026-04-14T11:44:00Z")
    Instant timestamp) {
}
