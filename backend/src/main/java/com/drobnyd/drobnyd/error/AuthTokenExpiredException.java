package com.drobnyd.drobnyd.error;

public class AuthTokenExpiredException extends ApiException {

    public AuthTokenExpiredException(String detail) {
        super(ProblemDescriptor.AUTH_TOKEN_EXPIRED, detail);
    }
}
