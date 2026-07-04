package com.drobnyd.drobnyd.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthenticatedUserResponse", description = "Authenticated user profile returned after session creation or refresh.")
public record AuthenticatedUserResponse(
                @Schema(example = "42") Integer localId,
                @Schema(example = "firebase-uid-123") String firebaseUid,
                @Schema(example = "jan@example.com") String email,
                @Schema(example = "Jan Kowalski") String displayName,
                @Schema(example = "CLIENT") String accountType,
                @Schema(example = "ROLE_CLIENT") String role,
                @Schema(example = "7", nullable = true) Integer printingPointId,
                @ArraySchema(schema = @Schema(example = "ROLE_CLIENT")) List<String> authorities) {
}
