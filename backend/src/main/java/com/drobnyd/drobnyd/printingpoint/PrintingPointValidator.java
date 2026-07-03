package com.drobnyd.drobnyd.printingpoint;

import com.drobnyd.drobnyd.api.ApiFieldError;
import com.drobnyd.drobnyd.printingpoint.dto.PrintingPointRequest;
import com.drobnyd.drobnyd.validation.RequestValidator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrintingPointValidator extends RequestValidator<PrintingPointRequest> {

    @Override
    protected void validateCustom(PrintingPointRequest request, List<ApiFieldError> errors) {
        if (!"poland".equalsIgnoreCase(request.country().trim())) {
            errors.add(new ApiFieldError(
                "country",
                "must be Poland",
                "Punkt druku musi znajdować się w Polsce."
            ));
        }
    }

    @Override
    protected String resolveUserHint(String fieldName, String constraintCode) {
        if ("postalCode".equals(fieldName) && "Pattern".equals(constraintCode)) {
            return "Podaj kod pocztowy w formacie 00-000.";
        }

        if ("hourlyOrderLimit".equals(fieldName) && "Min".equals(constraintCode)) {
            return "Limit zamówień musi być większy od zera.";
        }

        return super.resolveUserHint(fieldName, constraintCode);
    }
}
