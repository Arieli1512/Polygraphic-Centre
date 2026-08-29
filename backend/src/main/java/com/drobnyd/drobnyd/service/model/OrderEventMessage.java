package com.drobnyd.drobnyd.service.model;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

public record OrderEventMessage(
        String eventId,
        OrderEventType eventType,
        OffsetDateTime occurredAt,
        @Nullable Integer orderId,
        @Nullable Integer clientId,
        @Nullable String clientEmail,
        @Nullable Integer printingPointId,
        @Nullable String filePath,
        @Nullable String previousStatus,
        @Nullable String currentStatus,
        @Nullable String reason,
        @Nullable String requestId) {
}
