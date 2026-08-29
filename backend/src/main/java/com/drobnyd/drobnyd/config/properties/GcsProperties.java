package com.drobnyd.drobnyd.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gcs")
public record GcsProperties(
        String projectId,
        String bucketName,
        long signedUrlTtlMinutes,
        String uploadRootPrefix,
        String signingServiceAccountEmail) {

    public GcsProperties {
        projectId = defaultString(projectId, "");
        bucketName = defaultString(bucketName, "");
        signedUrlTtlMinutes = signedUrlTtlMinutes <= 0 ? 15 : signedUrlTtlMinutes;
        uploadRootPrefix = defaultString(uploadRootPrefix, "clients");
        signingServiceAccountEmail = defaultString(signingServiceAccountEmail, "");
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
