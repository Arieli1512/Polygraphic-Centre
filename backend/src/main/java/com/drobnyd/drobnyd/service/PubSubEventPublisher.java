package com.drobnyd.drobnyd.service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.drobnyd.drobnyd.config.properties.PubSubProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class PubSubEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PubSubEventPublisher.class);

    private final PubSubProperties pubSubProperties;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Publisher> publishersByTopic = new ConcurrentHashMap<>();

    public PubSubEventPublisher(PubSubProperties pubSubProperties, ObjectMapper objectMapper) {
        this.pubSubProperties = pubSubProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void logConfiguration() {
        log.info(
                "Pub/Sub configuration: enabled={} projectIdPresent={} orderTopic={} notificationTopic={} storageTopic={}",
                pubSubProperties.enabled(),
                !pubSubProperties.projectId().isBlank(),
                pubSubProperties.orderEventsTopicName(),
                pubSubProperties.notificationEventsTopicName(),
                pubSubProperties.storageUploadsTopicName());
    }

    public void publish(String topicName, Object payload, Map<String, String> attributes) {
        if (!pubSubProperties.enabled()) {
            log.info("Pub/Sub disabled - skipping publish for topic={} payloadType={}", topicName,
                    payload.getClass().getSimpleName());
            return;
        }

        if (topicName == null || topicName.isBlank()) {
            log.warn("Pub/Sub topic name is blank - dropping event payloadType={}", payload.getClass().getSimpleName());
            return;
        }

        try {
            Publisher publisher = publishersByTopic.computeIfAbsent(topicName, this::createPublisher);
            String json = toJson(payload);

            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(json))
                    .putAllAttributes(attributes == null ? Map.of() : attributes)
                    .build();

            ApiFuture<String> future = publisher.publish(message);
            String messageId = future.get(10, TimeUnit.SECONDS);
            log.info("Published event to topic={} messageId={} payloadType={}",
                    topicName,
                    messageId,
                    payload.getClass().getSimpleName());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish event to Pub/Sub topic " + topicName, ex);
        }
    }

    private Publisher createPublisher(String topicName) {
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(pubSubProperties.projectId(), topicName);
            return Publisher.newBuilder(projectTopicName).build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create Pub/Sub publisher for topic " + topicName, ex);
        }
    }

    private String toJson(Object payload) {
        Objects.requireNonNull(payload, "payload must not be null");

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Pub/Sub payload", ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        publishersByTopic.forEach((topic, publisher) -> {
            try {
                publisher.shutdown();
                publisher.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while shutting down publisher for topic={}", topic);
            }
        });
    }
}
