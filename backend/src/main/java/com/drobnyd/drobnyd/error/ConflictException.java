package com.drobnyd.drobnyd.error;

public class ConflictException extends ApiException {

    public ConflictException(String detail) {
        super(ProblemDescriptor.CONFLICT, detail);
    }
}
