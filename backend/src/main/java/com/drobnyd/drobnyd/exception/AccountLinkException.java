package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

public final class AccountLinkException extends ApiException {

    public AccountLinkException(String detail) {
        super(
                HttpStatus.CONFLICT,
                "https://api.polygraphic-centre.dev/problems/account-link-conflict",
                "ACCOUNT_LINK_CONFLICT",
                detail,
                "Twoje konto logowania nie jest poprawnie powiazane z kontem aplikacji.",
                "Skontaktuj sie z administratorem lub odtworz lokalne powiazanie konta.",
                false,
                null,
                null);
    }
}