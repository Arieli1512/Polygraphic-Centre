package com.drobnyd.drobnyd.printingpoint.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrintingPointRequest(
    @NotBlank
    @Size(max = 255)
    String name,

    @NotBlank
    @Size(max = 255)
    String streetAddress,

    @NotBlank
    @Size(max = 100)
    String city,

    @NotBlank
    @Pattern(regexp = "^[0-9]{2}-[0-9]{3}$")
    String postalCode,

    @NotBlank
    @Size(max = 100)
    String country,

    @NotNull
    @Min(1)
    @Max(40)
    Integer hourlyOrderLimit
) {
}
