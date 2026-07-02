package com.drobnyd.drobnyd.printingpoint;

import com.drobnyd.drobnyd.printingpoint.dto.PrintingPointRequest;
import com.drobnyd.drobnyd.validation.RequestValidator;
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
