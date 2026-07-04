package com.drobnyd.drobnyd.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;
    private final String userMessage;
    private final String action;
    private final boolean retryable;
    private final @Nullable String docs;

    protected ApiException(
            HttpStatus status,
            String type,
            String code,
            String detail,
            String userMessage,
            String action,
            boolean retryable,
            @Nullable String docs,
            @Nullable Throwable cause) {
        super(detail, cause);
        this.status = status;
        this.type = type;
        this.code = code;
        this.userMessage = userMessage;
        this.action = action;
        this.retryable = retryable;
        this.docs = docs;
    }

    public HttpStatus status() {
        return status;
    }

    public String type() {
        return type;
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

    public @Nullable String docs() {
        return docs;
    }
}