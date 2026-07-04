package com.drobnyd.drobnyd.service.model;

public record PriceEstimate(
        Integer printingPointId,
        long unitPagePrice,
        int billableSheets,
        int copies,
        long basePrice,
        long extrasPrice,
        long totalPrice,
        String currency) {
}