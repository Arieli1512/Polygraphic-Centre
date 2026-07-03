package com.drobnyd.drobnyd.printingpoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Printing point response.")
public record PrintingPointResponse(
    @Schema(example = "1")
    Integer printingPointId,

    @Schema(example = "Main Printing Point")
    String name,

    @Schema(example = "Example Street 1")
    String streetAddress,

    @Schema(example = "Warsaw")
    String city,

    @Schema(example = "00-001")
    String postalCode,

    @Schema(example = "Poland")
    String country,

    @Schema(example = "20")
    Integer hourlyOrderLimit
) {
}
