package com.drobnyd.drobnyd.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

public final class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException(String detail) {
        this(detail, null);
    }

    public AuthenticationFailedException(String detail, @Nullable Throwable cause) {
        super(
                HttpStatus.UNAUTHORIZED,
                "https://api.polygraphic-centre.dev/problems/authentication-failed",
                "AUTHENTICATION_FAILED",
                detail,
                "Nie udalo sie potwierdzic tozsamosci uzytkownika.",
                "Zaloguj sie ponownie i sprobuj jeszcze raz.",
                false,
                null,
                cause);
    }
}