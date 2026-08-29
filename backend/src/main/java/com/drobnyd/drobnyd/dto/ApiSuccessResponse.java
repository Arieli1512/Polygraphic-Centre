package com.drobnyd.drobnyd.dto;

public record ApiSuccessResponse<T>(
        T data,
        ApiResponseMeta meta) {
}