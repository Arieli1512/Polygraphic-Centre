package com.drobnyd.drobnyd.error;

import com.drobnyd.drobnyd.api.ApiProblemResponse;
import com.drobnyd.drobnyd.api.ApiFieldError;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
     * Obsługuje kontrolowane wyjątki aplikacyjne.
     * Każdy ApiException wskazuje ProblemDescriptor, a opcjonalnie przenosi dynamiczne dane,
     * takie jak detail, action override albo lista błędów pól.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiProblemResponse> handleApiException(
        ApiException exception,
        HttpServletRequest request
    ) {
        ProblemDescriptor descriptor = exception.getProblemDescriptor();

        return ApiProblemBuilder
            .slug(descriptor.slug())
            .title(descriptor.title())
            .status(descriptor.status())
            .code(descriptor.code())
            .detail(exception.getMessage())
            .userMessage(exception.getUserMessage())
            .action(exception.getAction())
            .validationErrors(exception.getErrors())
            .retryable(exception.isRetryable())
            .toResponseEntity(request, traceContextProvider);
    }

    /**
     * Obsługuje żądania do nieistniejącej ścieżki API.
     * To nie jest brak zasobu domenowego, tylko literówka albo nieaktualny adres endpointu.
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
     * Obsługuje walidację parametrów metody kontrolera, najczęściej query params.
     * Przykład: page=-1 albo size=500 przy adnotacjach @Min/@Max.
     * Zbiera wszystkie naruszenia, żeby klient dostał pełną listę problemów.
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
     * Obsługuje brak wymaganego parametru query.
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
     * Obsługuje query params, których Spring nie potrafi przekonwertować na oczekiwany typ.
     * Przykład: page=abc, gdy kontroler oczekuje int.
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
     * Obsługuje niepoprawne body requestu przed uruchomieniem Bean Validation.
     * Przykład: błędny JSON, pusty body przy @RequestBody albo wartość, której nie da się zmapować na DTO.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiProblemResponse> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return ApiProblemBuilder
            .slug("invalid-request-body")
            .title("Invalid Request Body")
            .status(HttpStatus.BAD_REQUEST)
            .code(ErrorCodes.INVALID_REQUEST_BODY)
            .detail("Request body is missing or malformed.")
            .userMessage("Treść żądania jest nieprawidłowa.")
            .action("Popraw JSON w body requestu i spróbuj ponownie.")
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
     * Ostatnia linia obrony dla nieprzewidzianych błędów.
     * Nie ujawnia szczegółów technicznych klientowi, ale nadal zwraca odpowiedź zgodną z Problem Details.
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
