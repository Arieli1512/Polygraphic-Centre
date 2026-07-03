package com.drobnyd.drobnyd.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single validation field error.")
public record ApiFieldError(
    @Schema(example = "postalCode")
    String field,

    @Schema(example = "must match \"^[0-9]{2}-[0-9]{3}$\"")
    String issue,

    @Schema(example = "Podaj kod pocztowy w formacie 00-000.")
    String userHint
) {
}
