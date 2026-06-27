package com.drobnyd.drobnyd.dto;

public record ApiFieldError(
    String field,
    String issue,
    String userHint
) {
}
