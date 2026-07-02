package com.drobnyd.drobnyd.api;

public record ApiResponse<T>(
    T data,
    ApiMeta meta
) {}