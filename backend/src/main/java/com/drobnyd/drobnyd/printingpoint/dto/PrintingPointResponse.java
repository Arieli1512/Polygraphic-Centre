package com.drobnyd.drobnyd.printingpoint.dto;

public record PrintingPointResponse(
    Integer printingPointId,
    String name,
    String streetAddress,
    String city,
    String postalCode,
    String country,
    Integer hourlyOrderLimit
) {
}