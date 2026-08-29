package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

public class WalletUnavailableException extends ApiException {

    public WalletUnavailableException(Integer clientId, String reason) {
        super(
                HttpStatus.CONFLICT,
                "https://api.polygraphic-centre.dev/problems/wallet-unavailable",
                "WALLET_UNAVAILABLE",
                "Wallet for client %d is unavailable: %s".formatted(clientId, reason),
                "Portfel klienta jest chwilowo niedostepny dla tej operacji.",
                "Sprawdz status portfela albo skontaktuj sie z administratorem.",
                false,
                null,
                null);
    }
}