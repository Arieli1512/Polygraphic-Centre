package com.drobnyd.drobnyd.api;

import java.time.Instant;

public record ApiMeta(
    String requestId,
    Instant timestamp
) {}