package com.drobnyd.drobnyd.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pubsub")
public record PubSubProperties(
        String projectId,
        String orderEventsTopicName,
        String notificationEventsTopicName,
        String storageUploadsTopicName,
        boolean enabled,
        boolean requireOidc,
        String webhookAudience) {

    public PubSubProperties {
        projectId = defaultString(projectId, "");
        orderEventsTopicName = defaultString(orderEventsTopicName, "order-events");
        notificationEventsTopicName = defaultString(notificationEventsTopicName, "notification-events");
        storageUploadsTopicName = defaultString(storageUploadsTopicName, "storage-uploads");
        webhookAudience = defaultString(webhookAudience, "");
        enabled = enabled && !projectId.isBlank();
    }

    private static String defaultString(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }
}
