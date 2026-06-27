package com.drobnyd.drobnyd.dto;

import java.time.Instant;

public record ApiMeta(
    String requestId,
    Instant timestamp
) {}