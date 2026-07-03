package com.drobnyd.drobnyd.error;

import com.drobnyd.drobnyd.api.ApiFieldError;

import java.util.List;

public abstract class ApiException extends RuntimeException {

    private final ProblemDescriptor problemDescriptor;
    private List<ApiFieldError> errors;
    private String userMessage;
    private String action;
    private Boolean retryable;

    protected ApiException(ProblemDescriptor problemDescriptor, String detail) {
        super(detail);
        this.problemDescriptor = problemDescriptor;
    }

    protected ApiException(
        ProblemDescriptor problemDescriptor,
        String detail,
        List<ApiFieldError> errors
    ) {
        this(problemDescriptor, detail);
        this.errors = errors;
    }

    public ApiException userMessage(String userMessage) {
        this.userMessage = userMessage;
        return this;
    }

    public ApiException action(String action) {
        this.action = action;
        return this;
    }

    public ApiException retryable(boolean retryable) {
        this.retryable = retryable;
        return this;
    }

    public ProblemDescriptor getProblemDescriptor() {
        return problemDescriptor;
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }

    public String getUserMessage() {
        return userMessage != null ? userMessage : problemDescriptor.userMessage();
    }

    public String getAction() {
        return action != null ? action : problemDescriptor.action();
    }

    public boolean isRetryable() {
        return retryable != null ? retryable : problemDescriptor.retryable();
    }
}
