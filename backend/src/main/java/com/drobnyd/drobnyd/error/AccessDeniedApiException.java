package com.drobnyd.drobnyd.error;

public class AccessDeniedApiException extends ApiException {

    public AccessDeniedApiException(String detail) {
        super(ProblemDescriptor.ACCESS_DENIED, detail);
    }
}
