package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

public class ConfigurationConflictException extends ApiException {

    public ConfigurationConflictException(String detail, String userMessage, String action) {
        super(
                HttpStatus.CONFLICT,
                "https://api.polygraphic-centre.dev/problems/configuration-conflict",
                "CONFIGURATION_CONFLICT",
                detail,
                userMessage,
                action,
                false,
                null,
                null);
    }
}
