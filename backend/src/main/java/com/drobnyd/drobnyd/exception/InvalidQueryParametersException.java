package com.drobnyd.drobnyd.exception;

import com.drobnyd.drobnyd.dto.ApiFieldError;

import java.util.List;

public class InvalidQueryParametersException extends RuntimeException {

    private final List<ApiFieldError> errors;

    public InvalidQueryParametersException(List<ApiFieldError> errors) {
        super("Invalid query parameters.");
        this.errors = errors;
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }
}