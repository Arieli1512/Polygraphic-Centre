package com.drobnyd.drobnyd.dto;

public record ApiResponse<T>(
    T data,
    ApiMeta meta
) {}