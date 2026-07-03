package com.drobnyd.drobnyd.printingpoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Printing point create/update request.")
public record PrintingPointRequest(
    @Schema(example = "Main Printing Point")
    @NotBlank
    @Size(max = 255)
    String name,

    @Schema(example = "Example Street 1")
    @NotBlank
    @Size(max = 255)
    String streetAddress,

    @Schema(example = "Warsaw")
    @NotBlank
    @Size(max = 100)
    String city,

    @Schema(example = "00-001", pattern = "^[0-9]{2}-[0-9]{3}$")
    @NotBlank
    @Pattern(regexp = "^[0-9]{2}-[0-9]{3}$")
    String postalCode,

    @Schema(example = "Poland")
    @NotBlank
    @Size(max = 100)
    String country,

    @Schema(example = "20", minimum = "1", maximum = "40")
    @NotNull
    @Min(1)
    @Max(40)
    Integer hourlyOrderLimit
) {
}
