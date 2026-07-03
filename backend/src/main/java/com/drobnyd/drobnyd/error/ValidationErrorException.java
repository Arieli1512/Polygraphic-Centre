package com.drobnyd.drobnyd.error;

import com.drobnyd.drobnyd.api.ApiFieldError;

import java.util.List;

public class ValidationErrorException extends ApiException {

    public ValidationErrorException(List<ApiFieldError> errors) {
        super(
            ProblemDescriptor.VALIDATION_ERROR,
            "Invalid request body.",
            errors
        );
    }
}
