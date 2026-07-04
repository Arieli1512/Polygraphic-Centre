package com.drobnyd.drobnyd.service.model;

public record ExtraPricingSnapshot(
        long bindingPrice,
        long staplingPrice,
        long coverPrice) {
}