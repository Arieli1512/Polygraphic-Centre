package com.drobnyd.drobnyd.service.model;

public record BalanceCheckResult(
        Integer clientId,
        long currentBalance,
        long requiredAmount,
        boolean sufficient) {
}