package com.drobnyd.drobnyd.api;

public record ApiFieldError(
    String field,
    String issue,
    String userHint
) {
}
