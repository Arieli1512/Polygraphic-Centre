package com.drobnyd.drobnyd.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public record NotificationMailProperties(
        boolean enabled,
        String fromAddress,
        String subjectPrefix) {

    public NotificationMailProperties {
        fromAddress = defaultString(fromAddress, "no-reply@polygraphic-centre.local");
        subjectPrefix = defaultString(subjectPrefix, "[Polygraphic Centre]");
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
