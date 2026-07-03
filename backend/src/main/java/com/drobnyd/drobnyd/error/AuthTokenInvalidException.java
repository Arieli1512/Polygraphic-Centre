package com.drobnyd.drobnyd.error;

public class AuthTokenInvalidException extends ApiException {

    public AuthTokenInvalidException(String detail) {
        super(ProblemDescriptor.AUTH_TOKEN_INVALID, detail);
    }
}
