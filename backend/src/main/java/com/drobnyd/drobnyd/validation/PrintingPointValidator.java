package com.drobnyd.drobnyd.validation;

import com.drobnyd.drobnyd.dto.PrintingPointRequest;
import org.springframework.stereotype.Component;

@Component
public class PrintingPointValidator extends RequestValidator<PrintingPointRequest> {

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
