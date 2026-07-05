package com.drobnyd.drobnyd.orders.events;

public record OrderStatusChangedEvent(
        Integer orderId,
        String clientEmail,
        String newStatus
) {}