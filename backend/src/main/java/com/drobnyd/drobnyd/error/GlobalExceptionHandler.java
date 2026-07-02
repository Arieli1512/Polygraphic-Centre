package com.drobnyd.drobnyd.error;

import com.drobnyd.drobnyd.api.ApiProblemResponse;
import com.drobnyd.drobnyd.api.ApiFieldError;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TraceContextProvider traceContextProvider;

    public GlobalExceptionHandler(TraceContextProvider traceContextProvider) {
        this.traceContextProvider = traceContextProvider;
    }

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
            .toResponseEntity(request, traceContextProvider);
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
            .toResponseEntity(request, traceContextProvider);
    }

    /**
     * Nieprawidłowe query params wykryte przez Bean Validation.
     * Akumuluje wszystkie naruszenia z walidacji parametrów metody.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiProblemResponse> handleHandlerMethodValidation(
        HandlerMethodValidationException exception,
        HttpServletRequest request
    ) {
        List<ApiFieldError> errors = exception.getParameterValidationResults()
            .stream()
            .flatMap(result -> result.getResolvableErrors()
                .stream()
                .map(error -> new ApiFieldError(
                    parameterName(result),
                    error.getDefaultMessage(),
                    null
                )))
            .toList();

        return invalidQueryParametersResponse(
            "Invalid query parameters.",
            errors,
            request
        );
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
            .toResponseEntity(request, traceContextProvider);
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
            .toResponseEntity(request, traceContextProvider);
    }

    /**
     * Nieprawidłowe dane formularza albo body requestu.
     */
    @ExceptionHandler(ValidationErrorException.class)
    public ResponseEntity<ApiProblemResponse> handleInvalidRequest(
        ValidationErrorException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("validation-error")
            .title("Validation Error")
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .code(ErrorCodes.VALIDATION_ERROR)
            .detail(exception.getMessage())
            .userMessage("Nieprawidłowe dane formularza.")
            .action("Popraw wskazane pola i spróbuj ponownie.")
            .validationErrors(exception.getErrors())
            .retryable(false)
            .toResponseEntity(request, traceContextProvider);
    }

    private ResponseEntity<ApiProblemResponse> invalidQueryParametersResponse(
        String detail,
        List<ApiFieldError> errors,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("invalid-query-parameters")
            .title("Invalid Query Parameters")
            .status(HttpStatus.BAD_REQUEST)
            .code(ErrorCodes.INVALID_QUERY_PARAMETERS)
            .detail(detail)
            .userMessage("Nieprawidłowe parametry zapytania.")
            .action("Popraw parametry w adresie URL i spróbuj ponownie.")
            .validationErrors(errors)
            .retryable(false)
            .toResponseEntity(request, traceContextProvider);
    }

    private String parameterName(ParameterValidationResult result) {
        return result.getMethodParameter().getParameterName();
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
            .toResponseEntity(request, traceContextProvider);
    }
}
