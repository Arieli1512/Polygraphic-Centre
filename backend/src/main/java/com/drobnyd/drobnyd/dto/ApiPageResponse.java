package com.drobnyd.drobnyd.dto;

import java.util.List;

public record ApiPageResponse<T>(
        List<T> data,
        ApiPageMeta meta,
        ApiLinks links
) {
}