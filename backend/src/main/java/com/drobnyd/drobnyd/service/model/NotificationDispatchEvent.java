package com.drobnyd.drobnyd.service.model;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

public record NotificationDispatchEvent(
        String eventId,
        String sourceOrderEventId,
        OrderEventType sourceEventType,
        OffsetDateTime occurredAt,
        String channel,
        String status,
        @Nullable String recipient,
        @Nullable String detail,
        @Nullable String requestId) {
}
