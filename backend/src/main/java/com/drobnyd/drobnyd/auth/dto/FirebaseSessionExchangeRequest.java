package com.drobnyd.drobnyd.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FirebaseSessionExchangeRequest(
        @NotBlank(message = "Firebase ID token is required") String idToken
) {
}

