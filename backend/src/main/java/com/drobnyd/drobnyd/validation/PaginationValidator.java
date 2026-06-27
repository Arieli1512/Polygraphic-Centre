package com.drobnyd.drobnyd.validation;

import com.drobnyd.drobnyd.dto.ApiFieldError;
import com.drobnyd.drobnyd.exception.InvalidQueryParametersException;

import java.util.List;
import java.util.ArrayList;

public final class PaginationValidator {

    private static final int DEFAULT_MIN_PAGE = 0;
    private static final int DEFAULT_MIN_SIZE = 1;
    private static final int DEFAULT_MAX_SIZE = 100;

    private PaginationValidator() {
    }

    public static void validate(int page, int size) {
        validate(page, size, DEFAULT_MAX_SIZE);
    }

    public static void validate(int page, int size, int maxSize) {
        List<ApiFieldError> errors = new ArrayList<>();

        if (page < DEFAULT_MIN_PAGE) {
            errors.add(new ApiFieldError(
                    "page",
                    "must be greater than or equal to " + DEFAULT_MIN_PAGE,
                    null
            ));
        }

        if (size < DEFAULT_MIN_SIZE) {
            errors.add(new ApiFieldError(
                    "size",
                    "must be greater than or equal to " + DEFAULT_MIN_SIZE,
                    null
            ));
        }

        if (size > maxSize) {
            errors.add(new ApiFieldError(
                    "size",
                    "must be less than or equal to " + maxSize,
                    null
            ));
        }

        if (!errors.isEmpty()) {
            throw new InvalidQueryParametersException(errors);
        }
    }
}