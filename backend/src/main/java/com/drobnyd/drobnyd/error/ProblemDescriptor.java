package com.drobnyd.drobnyd.error;

import org.springframework.http.HttpStatus;

public enum ProblemDescriptor {
    RESOURCE_NOT_FOUND(
        "resource-not-found",
        "Resource Not Found",
        HttpStatus.NOT_FOUND,
        ErrorCodes.RESOURCE_NOT_FOUND,
        "Nie znaleziono wskazanego zasobu.",
        "Sprawdź identyfikator i spróbuj ponownie.",
        false
    ),
    VALIDATION_ERROR(
        "validation-error",
        "Validation Error",
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorCodes.VALIDATION_ERROR,
        "Nieprawidłowe dane formularza.",
        "Popraw wskazane pola i spróbuj ponownie.",
        false
    ),
    ACCESS_DENIED(
        "access-denied",
        "Access Denied",
        HttpStatus.FORBIDDEN,
        ErrorCodes.ACCESS_DENIED,
        "Nie masz uprawnień do wykonania tej operacji.",
        "Zaloguj się na konto z odpowiednią rolą lub skontaktuj się z administratorem.",
        false
    ),
    AUTH_TOKEN_INVALID(
        "auth-token-invalid",
        "Unauthorized",
        HttpStatus.UNAUTHORIZED,
        ErrorCodes.AUTH_TOKEN_INVALID,
        "Zaloguj się, aby wykonać tę operację.",
        "Prześlij poprawny token uwierzytelniający i spróbuj ponownie.",
        false
    ),
    AUTH_TOKEN_EXPIRED(
        "auth-token-expired",
        "Unauthorized",
        HttpStatus.UNAUTHORIZED,
        ErrorCodes.AUTH_TOKEN_EXPIRED,
        "Twoja sesja wygasła.",
        "Zaloguj się ponownie i spróbuj jeszcze raz.",
        false
    ),
    CONFLICT(
        "conflict",
        "Conflict",
        HttpStatus.CONFLICT,
        ErrorCodes.CONFLICT,
        "Nie można wykonać tej operacji w obecnym stanie zasobu.",
        "Odśwież dane i spróbuj ponownie.",
        false
    ),
    EXTERNAL_SERVICE_UNAVAILABLE(
        "external-service-unavailable",
        "Service Unavailable",
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCodes.EXTERNAL_SERVICE_UNAVAILABLE,
        "Usługa zewnętrzna jest chwilowo niedostępna.",
        "Spróbuj ponownie później.",
        true
    );

    private final String slug;
    private final String title;
    private final HttpStatus status;
    private final String code;
    private final String userMessage;
    private final String action;
    private final boolean retryable;

    ProblemDescriptor(
        String slug,
        String title,
        HttpStatus status,
        String code,
        String userMessage,
        String action,
        boolean retryable
    ) {
        this.slug = slug;
        this.title = title;
        this.status = status;
        this.code = code;
        this.userMessage = userMessage;
        this.action = action;
        this.retryable = retryable;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String userMessage() {
        return userMessage;
    }

    public String action() {
        return action;
    }

    public boolean retryable() {
        return retryable;
    }
}
