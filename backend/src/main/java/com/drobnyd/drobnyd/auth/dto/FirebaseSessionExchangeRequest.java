package com.drobnyd.drobnyd.auth.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FirebaseSessionExchangeRequest", description = "Payload used to exchange a Firebase ID token for local session cookies.")
public record FirebaseSessionExchangeRequest(
        @Schema(example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...", description = "Firebase ID token issued by Firebase Authentication.") @NotBlank(message = "Firebase ID token is required") String idToken,
        @Schema(example = "Jan", description = "First name provided during signup.") String firstName,
        @Schema(example = "Kowalski", description = "Last name provided during signup.") String lastName) {
}
