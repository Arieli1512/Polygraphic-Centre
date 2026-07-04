package com.drobnyd.drobnyd.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationIssue", description = "Single field-level validation issue.")
public record ApiValidationIssue(
        @Schema(example = "idToken") String field,
        @Schema(example = "must not be blank") String issue,
        @Schema(example = "Wprowadz token Firebase i sprobuj ponownie.") String userHint) {
}