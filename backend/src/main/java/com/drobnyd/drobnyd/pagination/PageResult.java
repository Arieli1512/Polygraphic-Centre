package com.drobnyd.drobnyd.pagination;

import java.util.List;

public record PageResult<T>(
    List<T> items,
    int page,
    int size,
    int totalItems,
    int totalPages
) {
}
