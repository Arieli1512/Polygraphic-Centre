package com.drobnyd.drobnyd.service.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record PrintShopDetails(
        Integer printingPointId,
        String name,
        String streetAddress,
        String city,
        String postalCode,
        String country,
        Integer hourlyOrderLimit,
        List<OpeningHoursWindow> openingHours,
        @Nullable ExtraPricingSnapshot extraPricing) {
}