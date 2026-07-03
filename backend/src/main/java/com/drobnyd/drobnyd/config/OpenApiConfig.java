package com.drobnyd.drobnyd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    public OpenAPI polygraphicCentreOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Polygraphic Centre API")
                .version("1.0.0")
                .description("REST API for Polygraphic Centre."))
            .addServersItem(new Server()
                .url("/"))
            .components(new Components()
                .addSchemas("ApiMeta", apiMetaSchema())
                .addSchemas("ApiPageMeta", apiPageMetaSchema())
                .addSchemas("ApiLinks", apiLinksSchema())
                .addSchemas("ApiFieldError", apiFieldErrorSchema())
                .addSchemas("ProblemDetails", problemDetailsSchema())
                .addSchemas("ValidationProblemDetails", validationProblemDetailsSchema())
                .addParameters("XRequestId", headerParameter(
                    "x-request-id",
                    "Business correlation request identifier. If omitted, backend generates one."
                ))
                .addParameters("Traceparent", headerParameter(
                    "traceparent",
                    "W3C Trace Context header used to continue distributed traces."
                ))
                .addParameters("Tracestate", headerParameter(
                    "tracestate",
                    "Optional W3C Trace Context vendor-specific state."
                ))
                .addResponses("BadRequest", problemResponse(
                    "Bad request",
                    "ProblemDetails",
                    badRequestExample()
                ))
                .addResponses("Unauthorized", problemResponse(
                    "Unauthorized",
                    "ProblemDetails",
                    unauthorizedExample()
                ))
                .addResponses("Forbidden", problemResponse(
                    "Forbidden",
                    "ProblemDetails",
                    forbiddenExample()
                ))
                .addResponses("NotFound", problemResponse(
                    "Resource not found",
                    "ProblemDetails",
                    notFoundExample()
                ))
                .addResponses("Conflict", problemResponse(
                    "Conflict",
                    "ProblemDetails",
                    conflictExample()
                ))
                .addResponses("ValidationError", problemResponse(
                    "Validation error",
                    "ValidationProblemDetails",
                    validationErrorExample()
                ))
                .addResponses("TooManyRequests", problemResponse(
                    "Too many requests",
                    "ProblemDetails",
                    tooManyRequestsExample()
                ))
                .addResponses("InternalError", problemResponse(
                    "Internal server error",
                    "ProblemDetails",
                    internalErrorExample()
                ))
                .addResponses("ServiceUnavailable", problemResponse(
                    "Service unavailable",
                    "ProblemDetails",
                    serviceUnavailableExample()
                )));
    }

    @Bean
    public OperationCustomizer correlationHeadersCustomizer() {
        return (operation, handlerMethod) -> operation
            .addParametersItem(parameterRef("XRequestId"))
            .addParametersItem(parameterRef("Traceparent"))
            .addParametersItem(parameterRef("Tracestate"));
    }

    private Schema<?> apiMetaSchema() {
        return new ObjectSchema()
            .addProperty("requestId", new StringSchema())
            .addProperty("timestamp", new DateTimeSchema());
    }

    private Schema<?> apiPageMetaSchema() {
        return new ObjectSchema()
            .addProperty("requestId", new StringSchema())
            .addProperty("page", new IntegerSchema())
            .addProperty("size", new IntegerSchema())
            .addProperty("totalItems", new IntegerSchema())
            .addProperty("totalPages", new IntegerSchema())
            .addProperty("timestamp", new DateTimeSchema());
    }

    private Schema<?> apiLinksSchema() {
        return new ObjectSchema()
            .addProperty("self", new StringSchema())
            .addProperty("next", new StringSchema().nullable(true))
            .addProperty("prev", new StringSchema().nullable(true));
    }

    private Schema<?> apiFieldErrorSchema() {
        return new ObjectSchema()
            .addProperty("field", new StringSchema())
            .addProperty("issue", new StringSchema())
            .addProperty("userHint", new StringSchema());
    }

    private Schema<?> problemDetailsSchema() {
        return new ObjectSchema()
            .addRequiredItem("type")
            .addRequiredItem("title")
            .addRequiredItem("status")
            .addRequiredItem("detail")
            .addRequiredItem("instance")
            .addRequiredItem("code")
            .addRequiredItem("userMessage")
            .addRequiredItem("action")
            .addRequiredItem("requestId")
            .addRequiredItem("traceId")
            .addRequiredItem("timestamp")
            .addProperty("type", new StringSchema())
            .addProperty("title", new StringSchema())
            .addProperty("status", new IntegerSchema())
            .addProperty("detail", new StringSchema())
            .addProperty("instance", new StringSchema())
            .addProperty("code", new StringSchema())
            .addProperty("userMessage", new StringSchema())
            .addProperty("action", new StringSchema())
            .addProperty("retryable", new BooleanSchema())
            .addProperty("requestId", new StringSchema())
            .addProperty("traceId", new StringSchema())
            .addProperty("timestamp", new DateTimeSchema());
    }

    private Schema<?> validationProblemDetailsSchema() {
        return problemDetailsSchema()
            .addProperty("errors", new ArraySchema().items(ref("ApiFieldError")));
    }

    private Parameter headerParameter(String name, String description) {
        return new Parameter()
            .name(name)
            .in("header")
            .description(description)
            .schema(new StringSchema());
    }

    private Parameter parameterRef(String parameterName) {
        return new Parameter().$ref("#/components/parameters/" + parameterName);
    }

    private ApiResponse problemResponse(
        String description,
        String schemaName,
        Map<String, Object> example
    ) {
        return new ApiResponse()
            .description(description)
            .content(new io.swagger.v3.oas.models.media.Content()
                .addMediaType(PROBLEM_JSON, new io.swagger.v3.oas.models.media.MediaType()
                    .schema(ref(schemaName))
                    .addExamples("default", new Example().value(example))));
    }

    private Map<String, Object> badRequestExample() {
        return problemExample(
            "invalid-query-parameters",
            "Invalid Query Parameters",
            400,
            "Query parameter has invalid type.",
            "/api/v1/printing-points?page=abc&size=20",
            "INVALID_QUERY_PARAMETERS",
            "Jeden z parametrów zapytania ma nieprawidłowy format.",
            "Popraw parametr w adresie URL i spróbuj ponownie.",
            List.of(Map.of(
                "field", "page",
                "issue", "invalid type",
                "userHint", "Podaj liczbę całkowitą."
            ))
        );
    }

    private Map<String, Object> unauthorizedExample() {
        return problemExample(
            "auth-token-invalid",
            "Unauthorized",
            401,
            "Authentication token is missing or invalid.",
            "/api/v1/printing-points",
            "AUTH_TOKEN_INVALID",
            "Zaloguj się, aby wykonać tę operację.",
            "Prześlij poprawny token uwierzytelniający i spróbuj ponownie."
        );
    }

    private Map<String, Object> forbiddenExample() {
        return problemExample(
            "access-denied",
            "Access Denied",
            403,
            "Access denied for this resource.",
            "/api/v1/printing-points/1",
            "ACCESS_DENIED",
            "Nie masz uprawnień do wykonania tej operacji.",
            "Zaloguj się na konto z odpowiednią rolą lub skontaktuj się z administratorem."
        );
    }

    private Map<String, Object> notFoundExample() {
        return problemExample(
            "resource-not-found",
            "Resource Not Found",
            404,
            "Printing point not found: 999",
            "/api/v1/printing-points/999",
            "RESOURCE_NOT_FOUND",
            "Nie znaleziono wskazanego zasobu.",
            "Sprawdź identyfikator i spróbuj ponownie."
        );
    }

    private Map<String, Object> conflictExample() {
        return problemExample(
            "order-status-transition-not-allowed",
            "Order Transition Not Allowed",
            409,
            "Order status transition is not allowed.",
            "/api/v1/orders/ord_01JX9YQ4/status",
            "ORDER_STATUS_TRANSITION_NOT_ALLOWED",
            "Nie można wykonać tej zmiany statusu zamówienia.",
            "Odśwież widok zamówienia i sprawdź jego aktualny status.",
            List.of(Map.of(
                "field", "status",
                "issue", "transition from READY to APPROVED is forbidden",
                "userHint", "Skontaktuj się z obsługą, jeśli to wygląda na pomyłkę."
            ))
        );
    }

    private Map<String, Object> validationErrorExample() {
        return problemExample(
            "validation-error",
            "Validation Error",
            422,
            "Invalid request body.",
            "/api/v1/printing-points",
            "VALIDATION_ERROR",
            "Nieprawidłowe dane formularza.",
            "Popraw wskazane pola i spróbuj ponownie.",
            List.of(
                Map.of(
                    "field", "postalCode",
                    "issue", "must match \"^[0-9]{2}-[0-9]{3}$\"",
                    "userHint", "Podaj kod pocztowy w formacie 00-000."
                ),
                Map.of(
                    "field", "hourlyOrderLimit",
                    "issue", "must be greater than or equal to 1",
                    "userHint", "Limit zamówień musi być większy od zera."
                )
            )
        );
    }

    private Map<String, Object> tooManyRequestsExample() {
        return problemExample(
            "too-many-requests",
            "Too Many Requests",
            429,
            "Request rate limit exceeded.",
            "/api/v1/printing-points",
            "TOO_MANY_REQUESTS",
            "Przekroczono limit zapytań.",
            "Odczekaj chwilę i spróbuj ponownie.",
            true
        );
    }

    private Map<String, Object> internalErrorExample() {
        return problemExample(
            "internal-error",
            "Internal Error",
            500,
            "Unexpected internal server error.",
            "/api/v1/printing-points",
            "INTERNAL_ERROR",
            "Wystąpił nieoczekiwany błąd.",
            "Spróbuj ponownie później albo skontaktuj się z obsługą."
        );
    }

    private Map<String, Object> serviceUnavailableExample() {
        return problemExample(
            "external-service-unavailable",
            "Service Unavailable",
            503,
            "External service is temporarily unavailable.",
            "/api/v1/files/upload-requests",
            "EXTERNAL_SERVICE_UNAVAILABLE",
            "Usługa zewnętrzna jest chwilowo niedostępna.",
            "Spróbuj ponownie później.",
            true
        );
    }

    private Map<String, Object> problemExample(
        String slug,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action
    ) {
        return problemExample(
            slug,
            title,
            status,
            detail,
            instance,
            code,
            userMessage,
            action,
            false,
            null
        );
    }

    private Map<String, Object> problemExample(
        String slug,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action,
        boolean retryable
    ) {
        return problemExample(
            slug,
            title,
            status,
            detail,
            instance,
            code,
            userMessage,
            action,
            retryable,
            null
        );
    }

    private Map<String, Object> problemExample(
        String slug,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action,
        List<Map<String, String>> errors
    ) {
        return problemExample(
            slug,
            title,
            status,
            detail,
            instance,
            code,
            userMessage,
            action,
            false,
            errors
        );
    }

    private Map<String, Object> problemExample(
        String slug,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action,
        boolean retryable,
        List<Map<String, String>> errors
    ) {
        Map<String, Object> example = new java.util.LinkedHashMap<>();
        example.put("type", "https://api.polygraphic-centre.dev/problems/" + slug);
        example.put("title", title);
        example.put("status", status);
        example.put("detail", detail);
        example.put("instance", instance);
        example.put("code", code);
        example.put("userMessage", userMessage);
        example.put("action", action);

        if (errors != null) {
            example.put("errors", errors);
        }

        example.put("retryable", retryable);
        example.put("requestId", "0f6b6ca8-95af-4e1f-9b0b-3e31f5f49f8e");
        example.put("traceId", "3e7e26f4d3f498f1");
        example.put("timestamp", "2026-04-14T11:44:00Z");

        return example;
    }

    private Schema<?> ref(String schemaName) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName);
    }
}
