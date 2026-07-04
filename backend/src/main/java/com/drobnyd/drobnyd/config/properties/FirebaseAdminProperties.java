package com.drobnyd.drobnyd.config.properties;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase.admin")
public record FirebaseAdminProperties(
        boolean useApplicationDefaultCredentials,
        @Nullable String serviceAccountResource) {

    public FirebaseAdminProperties {
        serviceAccountResource = serviceAccountResource == null || serviceAccountResource.isBlank()
                ? null
                : serviceAccountResource;
    }
}