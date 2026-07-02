package com.drobnyd.drobnyd.exception;

import com.drobnyd.drobnyd.dto.ApiFieldError;

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
