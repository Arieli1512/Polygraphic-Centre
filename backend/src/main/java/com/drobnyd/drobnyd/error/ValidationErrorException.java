package com.drobnyd.drobnyd.error;

import com.drobnyd.drobnyd.api.ApiFieldError;

import java.util.List;

public class ValidationErrorException extends RuntimeException {

    private final List<ApiFieldError> errors;

    public ValidationErrorException(List<ApiFieldError> errors) {
        super("Invalid request body.");
        this.errors = errors;
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }
}
