package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends ApiException {

    public InsufficientBalanceException(Integer clientId, long requiredAmount, long currentBalance) {
        super(
                HttpStatus.CONFLICT,
                "https://api.polygraphic-centre.dev/problems/insufficient-balance",
                "INSUFFICIENT_BALANCE",
                "Client %d has insufficient wallet balance. Required=%d, current=%d"
                        .formatted(clientId, requiredAmount, currentBalance),
                "Brakuje srodkow w portfelu do wykonania tej operacji.",
                "Doladuj portfel albo zmniejsz wartosc zamowienia i sprobuj ponownie.",
                false,
                null,
                null);
    }
}