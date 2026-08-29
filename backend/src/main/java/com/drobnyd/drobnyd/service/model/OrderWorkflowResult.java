package com.drobnyd.drobnyd.service.model;

import java.time.OffsetDateTime;

import com.drobnyd.drobnyd.entity.OrderStatus;

public record OrderWorkflowResult(
        Integer orderId,
        Integer clientId,
        Integer printingPointId,
        OrderStatus status,
        long totalPrice,
        OffsetDateTime pickupAt) {
}