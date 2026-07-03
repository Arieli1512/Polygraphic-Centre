package com.drobnyd.drobnyd.pagination;

import com.drobnyd.drobnyd.api.ApiLinks;
import jakarta.servlet.http.HttpServletRequest;

public final class ApiPaginationLinks {

    private ApiPaginationLinks() {
    }

    public static ApiLinks build(
            HttpServletRequest request,
            int page,
            int size,
            int totalPages
    ) {
        String basePath = request.getRequestURI();

        String self = basePath + "?page=" + page + "&size=" + size;

        String next = null;
        if (page + 1 < totalPages) {
            next = basePath + "?page=" + (page + 1) + "&size=" + size;
        }

        String prev = null;
        if (page > 0) {
            prev = basePath + "?page=" + (page - 1) + "&size=" + size;
        }

        return new ApiLinks(self, next, prev);
    }
}