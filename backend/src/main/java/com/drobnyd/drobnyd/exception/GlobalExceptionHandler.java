package com.drobnyd.drobnyd.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler for REST API error responses.
 * 
 * Converts exceptions to standardized Problem Details format (RFC 7807).
 * Includes request tracing for debugging and support.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ApiProblemFactory apiProblemFactory;

    public GlobalExceptionHandler(ApiProblemFactory apiProblemFactory) {
        this.apiProblemFactory = apiProblemFactory;
    }

    /**
     * Handle validation errors from request body validation.
     * 
     * Returns 400 Bad Request with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblemDetails> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("[{}] Request body validation failed: {}", requestId(), ex.getMessage());

        List<ApiValidationIssue> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiValidationIssue(
                        error.getField(),
                        error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        error.getDefaultMessage() != null ? error.getDefaultMessage()
                                : "Sprawdz wartosc pola i sprobuj ponownie."))
                .toList();

        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "https://api.polygraphic-centre.dev/problems/validation-error",
                "Validation Error",
                "Validation failed for request body.",
                "VALIDATION_ERROR",
                "Nie udalo sie zapisac danych, bo czesc pól jest niepoprawna.",
                "Popraw oznaczone pola i sprobuj ponownie.",
                request,
                false,
                fieldErrors,
                null);
    }

    /**
     * Handle constraint violation errors from path/query parameter validation.
     * 
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiProblemDetails> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.warn("[{}] Constraint violation: {}", requestId(), ex.getMessage());

        List<ApiValidationIssue> violations = ex.getConstraintViolations().stream()
                .map(violation -> new ApiValidationIssue(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getMessage()))
                .toList();

        return problem(
                HttpStatus.BAD_REQUEST,
                "https://api.polygraphic-centre.dev/problems/validation-error",
                "Validation Error",
                "Validation failed for request parameters.",
                "VALIDATION_ERROR",
                "Nie udalo sie przetworzyc zapytania, bo parametry sa niepoprawne.",
                "Sprawdz parametry zapytania i sprobuj ponownie.",
                request,
                false,
                violations,
                null);
    }

    /**
     * Handle Spring Security access denied exceptions.
     * 
     * Returns 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblemDetails> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("[{}] Access denied: {}", requestId(), ex.getMessage());

        return problem(
                HttpStatus.FORBIDDEN,
                "https://api.polygraphic-centre.dev/problems/access-denied",
                "Access Denied",
                "Access denied for this resource.",
                "ACCESS_DENIED",
                "Nie masz uprawnien do wykonania tej operacji.",
                "Zaloguj sie na konto z odpowiednia rola lub skontaktuj sie z administratorem.",
                request,
                false,
                null,
                null);
    }

    /**
     * Handle domain API exceptions.
     * 
     * Preserves the HTTP status and metadata from the exception.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiProblemDetails> handleApiException(
            ApiException ex,
            HttpServletRequest request) {

        log.warn("[{}] API exception: {} - {}", requestId(), ex.code(), ex.getMessage());

        return problem(
                ex.status(),
                ex.type(),
                titleForStatus(ex.status()),
                ex.getMessage(),
                ex.code(),
                ex.userMessage(),
                ex.action(),
                request,
                ex.retryable(),
                null,
                ex.docs());
    }

    /**
     * Handle all other exceptions.
     * 
     * Returns 500 Internal Server Error without exposing implementation details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblemDetails> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("[{}] Unhandled exception", requestId(), ex);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "https://api.polygraphic-centre.dev/problems/internal-server-error",
                "Internal Server Error",
                "An unexpected error occurred.",
                "INTERNAL_SERVER_ERROR",
                "Wystapil nieoczekiwany blad po stronie serwera.",
                "Sprobuj ponownie pozniej. Jesli problem sie powtarza, przekaz requestId do wsparcia.",
                request,
                false,
                null,
                null);
    }

    private ResponseEntity<ApiProblemDetails> problem(
            HttpStatus status,
            String type,
            String title,
            String detail,
            String code,
            String userMessage,
            String action,
            HttpServletRequest request,
            boolean retryable,
            List<ApiValidationIssue> additionalErrors,
            String docs) {
        ApiProblemDetails body = apiProblemFactory.create(
                status,
                type,
                title,
                detail,
                code,
                userMessage,
                action,
                request,
                retryable,
                additionalErrors,
                docs);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    private String titleForStatus(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "Authentication Failed";
            case CONFLICT -> "Conflict";
            case NOT_FOUND -> "Not Found";
            default -> status.getReasonPhrase();
        };
    }
}
