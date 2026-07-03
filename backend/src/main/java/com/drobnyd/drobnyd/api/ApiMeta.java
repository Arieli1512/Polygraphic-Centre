package com.drobnyd.drobnyd.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Technical metadata for successful response.")
public record ApiMeta(
    @Schema(example = "a9b1c77f-22d4-48f3-a3a8-7d9f3f8de3c2")
    String requestId,

    @Schema(example = "2026-04-14T11:42:22Z")
    Instant timestamp
) {
}
