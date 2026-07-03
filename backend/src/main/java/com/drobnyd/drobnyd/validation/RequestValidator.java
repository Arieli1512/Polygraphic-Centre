package com.drobnyd.drobnyd.validation;

import com.drobnyd.drobnyd.api.ApiFieldError;
import com.drobnyd.drobnyd.error.ValidationErrorException;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;

public abstract class RequestValidator<T> {

    public final void validate(T request, BindingResult bindingResult) {
        List<ApiFieldError> errors = new ArrayList<>(bindingResult.getFieldErrors()
            .stream()
            .map(error -> new ApiFieldError(
                error.getField(),
                error.getDefaultMessage(),
                resolveUserHint(error.getField(), error.getCode())
            ))
            .toList());

        validateCustom(request, errors);

        if (!errors.isEmpty()) {
            throw new ValidationErrorException(errors);
        }
    }

    protected void validateCustom(T request, List<ApiFieldError> errors) {
    }

    protected String resolveUserHint(String fieldName, String constraintCode) {
        if (constraintCode == null) {
            return null;
        }

        return switch (constraintCode) {
            case "NotBlank" -> "Uzupełnij to pole.";
            case "NotNull" -> "Podaj wymaganą wartość.";
            case "Size" -> "Sprawdź długość wpisanej wartości.";
            case "Min" -> "Podaj większą wartość.";
            case "Max" -> "Podaj mniejszą wartość.";
            case "Pattern" -> "Sprawdź wymagany format wartości.";
            case "Email" -> "Podaj poprawny adres e-mail.";
            case "Positive" -> "Podaj wartość większą od zera.";
            case "PositiveOrZero" -> "Podaj wartość równą zero albo większą.";
            case "Negative" -> "Podaj wartość mniejszą od zera.";
            case "NegativeOrZero" -> "Podaj wartość równą zero albo mniejszą.";
            case "Past" -> "Podaj datę z przeszłości.";
            case "PastOrPresent" -> "Podaj datę z przeszłości albo dzisiejszą.";
            case "Future" -> "Podaj datę z przyszłości.";
            case "FutureOrPresent" -> "Podaj datę przyszłą albo dzisiejszą.";
            default -> null;
        };
    }
}
