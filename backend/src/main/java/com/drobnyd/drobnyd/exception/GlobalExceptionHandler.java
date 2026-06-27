package com.drobnyd.drobnyd.exception;

import com.drobnyd.drobnyd.common.ApiProblemBuilder;
import com.drobnyd.drobnyd.constants.ErrorCodes;
import com.drobnyd.drobnyd.dto.ApiProblemResponse;
import com.drobnyd.drobnyd.dto.ApiFieldError;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Endpoint istnieje, ale konkretny zasób nie został znaleziony.
     * Na przykład: punkt druku o zadanym ID już nie istnieje.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiProblemResponse> handleResourceNotFoundException(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("resource-not-found")
            .title("Resource Not Found")
            .status(HttpStatus.NOT_FOUND)
            .code(ErrorCodes.RESOURCE_NOT_FOUND)
            .detail(exception.getMessage())
            .userMessage("Nie znaleziono wskazanego zasobu.")
            .action("Sprawdź identyfikator i spróbuj ponownie.")
            .retryable(false)
            .toResponseEntity(request);
    }

    /**
     * Ścieżka API nie istnieje, np. literówka w URL.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiProblemResponse> handleNoResourceFoundException(
        NoResourceFoundException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("endpoint-not-found")
            .title("Resource Not Found")
            .status(HttpStatus.NOT_FOUND)
            .code(ErrorCodes.RESOURCE_NOT_FOUND)
            .detail("Requested endpoint does not exist.")
            .userMessage("Nie znaleziono wskazanego endpointu.")
            .action("Sprawdź adres URL i spróbuj ponownie.")
            .retryable(false)
            .toResponseEntity(request);
    }

    /**
     * Nieprawidłowe query params w adresie URL.
     */
    @ExceptionHandler(InvalidQueryParametersException.class)
    public ResponseEntity<ApiProblemResponse> handleInvalidQueryParameters(
        InvalidQueryParametersException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("invalid-query-parameters")
            .title("Invalid Query Parameters")
            .status(HttpStatus.BAD_REQUEST)
            .code(ErrorCodes.INVALID_QUERY_PARAMETERS)
            .detail(exception.getMessage())
            .userMessage("Nieprawidłowe parametry zapytania.")
            .action("Popraw parametry w adresie URL i spróbuj ponownie.")
            .validationErrors(exception.getErrors())
            .retryable(false)
            .toResponseEntity(request);
    }

    /**
     * Brakuje wymaganego parametru query w adresie URL.
     * Na przykład: endpoint wymaga `city`, ale klient go nie przesłał.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiProblemResponse> handleMissingServletRequestParameter(
        MissingServletRequestParameterException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("invalid-query-parameters")
            .title("Invalid Query Parameters")
            .status(HttpStatus.BAD_REQUEST)
            .code(ErrorCodes.INVALID_QUERY_PARAMETERS)
            .detail("Required query parameter is missing.")
            .userMessage("Brakuje wymaganego parametru zapytania.")
            .action("Uzupełnij wymagany parametr w adresie URL i spróbuj ponownie.")
            .validationErrors(List.of(
                new ApiFieldError(
                    exception.getParameterName(),
                    "is required",
                    null
                )
            ))
            .retryable(false)
            .toResponseEntity(request);
    }

    /**
     * Parametr query ma nieprawidłowy typ lub format.
     * Na przykład: `page=abc`, gdy oczekiwany jest `int`.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiProblemResponse> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("invalid-query-parameters")
            .title("Invalid Query Parameters")
            .status(HttpStatus.BAD_REQUEST)
            .code(ErrorCodes.INVALID_QUERY_PARAMETERS)
            .detail("Query parameter has invalid type.")
            .userMessage("Jeden z parametrów zapytania ma nieprawidłowy format.")
            .action("Popraw parametr w adresie URL i spróbuj ponownie.")
            .validationErrors(List.of(
                new ApiFieldError(
                    exception.getName(),
                    "invalid type",
                    null
                )
            ))
            .retryable(false)
            .toResponseEntity(request);
    }


    /**
     * Fallback dla nieprzewidzianych błędów aplikacji.
     * Tak, żeby wszystkie błędy były zgodne z kontraktem API.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblemResponse> handleUnknownException(
        Exception exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("internal-error")
            .title("Internal Error")
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .code(ErrorCodes.INTERNAL_ERROR)
            .detail("Unexpected internal server error.")
            .userMessage("Wystąpił nieoczekiwany błąd.")
            .action("Spróbuj ponownie później albo skontaktuj się z obsługą.")
            .retryable(false)
            .toResponseEntity(request);
    }
}