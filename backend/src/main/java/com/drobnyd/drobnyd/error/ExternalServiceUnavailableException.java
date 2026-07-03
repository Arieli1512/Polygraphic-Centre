package com.drobnyd.drobnyd.error;

public class ExternalServiceUnavailableException extends ApiException {

    public ExternalServiceUnavailableException(String detail) {
        super(ProblemDescriptor.EXTERNAL_SERVICE_UNAVAILABLE, detail);
    }
}
