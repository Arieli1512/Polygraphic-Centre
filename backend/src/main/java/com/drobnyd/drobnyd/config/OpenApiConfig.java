package com.drobnyd.drobnyd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Polygraphic Centre API", version = "1.0.0", description = "Backend API for authentication, diagnostics, and future printing workflows.", contact = @Contact(name = "Polygraphic Centre Team")))
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "pc_access_token", description = "HttpOnly cookie carrying the short-lived access token.")
public class OpenApiConfig {

    @Bean
    public OpenAPI polygraphicCentreOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSchemas("ValidationIssue", validationIssueSchema())
                        .addSchemas("ProblemDetails", problemDetailsSchema())
                        .addSchemas("ValidationProblemDetails", validationProblemDetailsSchema())
                        .addResponses("BadRequestProblem", problemResponse("Malformed request.", "ProblemDetails"))
                        .addResponses("UnauthorizedProblem",
                                problemResponse("Authentication is required or token is invalid.", "ProblemDetails"))
                        .addResponses("AccessDeniedProblem",
                                problemResponse("Authenticated user lacks required permissions.", "ProblemDetails"))
                        .addResponses("NotFoundProblem",
                                problemResponse("Requested resource was not found.", "ProblemDetails"))
                        .addResponses("ConflictProblem",
                                problemResponse("Business state conflict prevents the operation.", "ProblemDetails"))
                        .addResponses("ValidationProblem",
                                problemResponse("Request validation failed.", "ValidationProblemDetails"))
                        .addResponses("TooManyRequestsProblem",
                                problemResponse("Rate limit has been exceeded.", "ProblemDetails"))
                        .addResponses("InternalServerErrorProblem",
                                problemResponse("Unexpected server-side failure.", "ProblemDetails"))
                        .addResponses("ServiceUnavailableProblem",
                                problemResponse("Temporarily unavailable dependency or service.", "ProblemDetails")));
    }

    private ApiResponse problemResponse(String description, String schemaName) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schemaName))));
    }

    private Schema<?> validationIssueSchema() {
        return new ObjectSchema()
                .addProperty("field", new StringSchema().example("idToken"))
                .addProperty("issue", new StringSchema().example("must not be blank"))
                .addProperty("userHint", new StringSchema().example("Wprowadz token Firebase i sprobuj ponownie."));
    }

    private Schema<?> problemDetailsSchema() {
        return new ObjectSchema()
                .addProperty("type",
                        new StringSchema().example("https://api.polygraphic-centre.dev/problems/validation-error"))
                .addProperty("title", new StringSchema().example("Validation Error"))
                .addProperty("status", new IntegerSchema().example(422))
                .addProperty("detail", new StringSchema().example("Validation failed for request body."))
                .addProperty("instance", new StringSchema().example("/api/auth/session"))
                .addProperty("code", new StringSchema().example("VALIDATION_ERROR"))
                .addProperty("userMessage",
                        new StringSchema().example("Nie udalo sie zapisac danych, bo czesc pol jest niepoprawna."))
                .addProperty("action", new StringSchema().example("Popraw oznaczone pola i sprobuj ponownie."))
                .addProperty("requestId", new StringSchema().example("0f6b6ca8-95af-4e1f-9b0b-3e31f5f49f8e"))
                .addProperty("traceId", new StringSchema().nullable(true).example("3e7e26f4d3f498f1"))
                .addProperty("timestamp", new StringSchema().format("date-time").example("2026-04-14T11:44:00Z"))
                .addProperty("errors",
                        new ArraySchema().items(new Schema<>().$ref("#/components/schemas/ValidationIssue"))
                                .nullable(true))
                .addProperty("retryable", new BooleanSchema().example(false))
                .addProperty("docs", new StringSchema().nullable(true))
                .addRequiredItem("type")
                .addRequiredItem("title")
                .addRequiredItem("status")
                .addRequiredItem("detail")
                .addRequiredItem("instance")
                .addRequiredItem("code")
                .addRequiredItem("userMessage")
                .addRequiredItem("action")
                .addRequiredItem("requestId")
                .addRequiredItem("timestamp");
    }

    private Schema<?> validationProblemDetailsSchema() {
        return new ObjectSchema()
                .allOf(java.util.List.of(
                        new Schema<>().$ref("#/components/schemas/ProblemDetails"),
                        new ObjectSchema().addProperty(
                                "errors",
                                new ArraySchema().items(new Schema<>().$ref("#/components/schemas/ValidationIssue")))))
                .addRequiredItem("errors");
    }
}