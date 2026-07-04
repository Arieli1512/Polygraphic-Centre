package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

import com.drobnyd.drobnyd.entity.OrderStatus;

public class InvalidOrderTransitionException extends ApiException {

    public InvalidOrderTransitionException(Integer orderId, OrderStatus currentStatus, OrderStatus targetStatus) {
        super(
                HttpStatus.CONFLICT,
                "https://api.polygraphic-centre.dev/problems/order-status-transition-not-allowed",
                "ORDER_STATUS_TRANSITION_NOT_ALLOWED",
                "Order %d cannot transition from %s to %s"
                        .formatted(orderId, currentStatus, targetStatus),
                "Nie mozna wykonac tej zmiany statusu zamowienia.",
                "Odswiez widok zamowienia i sprawdz jego aktualny status.",
                false,
                null,
                null);
    }
}