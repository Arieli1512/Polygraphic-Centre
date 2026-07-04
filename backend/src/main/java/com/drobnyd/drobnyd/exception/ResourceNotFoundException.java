package com.drobnyd.drobnyd.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, String resourceIdentifier) {
        super(
                HttpStatus.NOT_FOUND,
                "https://api.polygraphic-centre.dev/problems/resource-not-found",
                "RESOURCE_NOT_FOUND",
                resourceName + " not found for identifier: " + resourceIdentifier,
                "Nie znaleziono wymaganego zasobu.",
                "Odswiez dane i sprawdz, czy zasob nadal istnieje.",
                false,
                null,
                null);
    }
}