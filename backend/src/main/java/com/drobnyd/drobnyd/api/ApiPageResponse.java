package com.drobnyd.drobnyd.api;

import java.util.List;

public record ApiPageResponse<T>(
        List<T> data,
        ApiPageMeta meta,
        ApiLinks links
) {
}