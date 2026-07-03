package com.drobnyd.drobnyd.error;

import com.drobnyd.drobnyd.api.ApiProblemResponse;
import com.drobnyd.drobnyd.api.ApiFieldError;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ApiProblemBuilder {

    private static final String PROBLEM_BASE_URL =
        "https://api.polygraphic-centre.dev/problems/";

    private String slug;
    private String title;
    private HttpStatus status;
    private String code;
    private String detail;
    private String userMessage;
    private String action;
    private final List<ApiFieldError> errors = new ArrayList<>();
    private boolean retryable = false;

    private ApiProblemBuilder() {
    }

    public static ApiProblemBuilder slug(String slug) {
        ApiProblemBuilder builder = new ApiProblemBuilder();
        builder.slug = slug;
        return builder;
    }

    public ApiProblemBuilder title(String title) {
        this.title = title;
        return this;
    }

    public ApiProblemBuilder status(HttpStatus status) {
        this.status = status;
        return this;
    }

    public ApiProblemBuilder code(String code) {
        this.code = code;
        return this;
    }

    public ApiProblemBuilder detail(String detail) {
        this.detail = detail;
        return this;
    }

    public ApiProblemBuilder userMessage(String userMessage) {
        this.userMessage = userMessage;
        return this;
    }

    public ApiProblemBuilder action(String action) {
        this.action = action;
        return this;
    }

    public ApiProblemBuilder validationErrors(List<ApiFieldError> errors) {
        if (errors != null) {
            this.errors.addAll(errors);
        }

        return this;
    }

    public ApiProblemBuilder retryable(boolean retryable) {
        this.retryable = retryable;
        return this;
    }

    public ResponseEntity<ApiProblemResponse> toResponseEntity(
        HttpServletRequest request,
        TraceContextProvider traceContextProvider
    ) {
        ApiProblemResponse response = new ApiProblemResponse(
            PROBLEM_BASE_URL + slug,
            title,
            status.value(),
            detail,
            request.getRequestURI(),
            code,
            userMessage,
            action,
            errors.isEmpty() ? null : errors,
            retryable,
            getRequestId(request),
            getTraceId(traceContextProvider),
            Instant.now()
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }


    private static String getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");

        return requestId.toString();
    }

    private static String getTraceId(TraceContextProvider traceContextProvider) {
        return traceContextProvider.currentTraceId();
    }
}
