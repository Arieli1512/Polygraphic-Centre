package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

public class InvalidReportRangeException extends ApiException {

    public InvalidReportRangeException(String detail) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "https://api.polygraphic-centre.dev/problems/invalid-report-range",
                "INVALID_REPORT_RANGE",
                detail,
                "Nie mozna wygenerowac raportu dla podanego zakresu dat.",
                "Sprawdz daty od-do i sproboj ponownie.",
                false,
                null,
                null);
    }
}
