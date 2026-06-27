package com.drobnyd.drobnyd.exception;

import com.drobnyd.drobnyd.constants.ErrorCodes;
import com.drobnyd.drobnyd.dto.ApiProblemResponse;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Endpoint istnieje, ale konkretny zasób nie został znaleziony.
     * Na przykład: punkt druku o zadanym ID już nie istnieje
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiProblemResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiProblemResponse response = new ApiProblemResponse(
                "https://api.polygraphic-centre.dev/problems/resource-not-found",
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                request.getRequestURI(),
                ErrorCodes.RESOURCE_NOT_FOUND,
                "Nie znaleziono wskazanego zasobu.",
                "Sprawdź identyfikator i spróbuj ponownie.",
                false,
                getRequestId(request),
                getTraceId(request),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    /**
     * Ścieżka API nie istnieje, np. literówka w URL.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiProblemResponse> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        ApiProblemResponse response = new ApiProblemResponse(
                "https://api.drobnyd.pl/problems/endpoint-not-found",
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                "Requested endpoint does not exist.",
                request.getRequestURI(),
                ErrorCodes.RESOURCE_NOT_FOUND,
                "Nie znaleziono wskazanego endpointu.",
                "Sprawdź adres URL i spróbuj ponownie.",
                false,
                getRequestId(request),
                getTraceId(request),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    /**
    * Fallback dla nieprzewidzianych błędów aplikacji.
    * Tak, że wszystkie błędy są zgodne z kontraktem API.
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblemResponse> handleUnknownException(
            Exception exception,
            HttpServletRequest request
    ) {
        ApiProblemResponse response = new ApiProblemResponse(
                "https://api.drobnyd.pl/problems/internal-error",
                "Internal Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected internal server error.",
                request.getRequestURI(),
                ErrorCodes.INTERNAL_ERROR,
                "Wystąpił nieoczekiwany błąd.",
                "Spróbuj ponownie później albo skontaktuj się z obsługą.",
                false,
                getRequestId(request),
                getTraceId(request),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    private String getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");

        return requestId.toString();
    }

    private String getTraceId(HttpServletRequest request) {
        // TODO: zaimplementować prawdziwy error tracing
        return getRequestId(request);
    }
}