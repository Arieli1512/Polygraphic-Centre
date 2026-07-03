package com.drobnyd.drobnyd.error;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ProblemDescriptor.RESOURCE_NOT_FOUND, message);
    }
}
