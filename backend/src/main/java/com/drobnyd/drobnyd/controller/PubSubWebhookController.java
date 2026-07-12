package com.drobnyd.drobnyd.controller;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.config.properties.PubSubProperties;
import com.drobnyd.drobnyd.service.NotificationService;
import com.drobnyd.drobnyd.service.NotificationWorkerService;
import com.drobnyd.drobnyd.service.PubSubOidcTokenValidator;
import com.drobnyd.drobnyd.service.model.OrderEventMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/webhooks/pubsub")
public class PubSubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PubSubWebhookController.class);

    private final ObjectMapper objectMapper;
    private final NotificationWorkerService notificationWorkerService;
    private final NotificationService notificationService;
    private final PubSubOidcTokenValidator pubSubOidcTokenValidator;
    private final PubSubProperties pubSubProperties;

    public PubSubWebhookController(
            ObjectMapper objectMapper,
            NotificationWorkerService notificationWorkerService,
            NotificationService notificationService,
            PubSubOidcTokenValidator pubSubOidcTokenValidator,
            PubSubProperties pubSubProperties) {
        this.objectMapper = objectMapper;
        this.notificationWorkerService = notificationWorkerService;
        this.notificationService = notificationService;
        this.pubSubOidcTokenValidator = pubSubOidcTokenValidator;
        this.pubSubProperties = pubSubProperties;
    }

    @PostMapping("/order-events")
    public ResponseEntity<Void> consumeOrderEvents(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody PubSubPushRequest request) {
        if (!pubSubOidcTokenValidator.isValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String requestId = requestId();
        try {
            String json = decodeData(request.message().data());
            OrderEventMessage eventMessage = objectMapper.readValue(json, OrderEventMessage.class);
            log.info("[{}] Received order-event webhook messageId={} eventType={} orderId={}",
                    requestId,
                    request.message().messageId(),
                    eventMessage.eventType(),
                    eventMessage.orderId());

            notificationWorkerService.handleOrderEvent(eventMessage);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("[{}] Failed to process order-event webhook messageId={}", requestId,
                    request.message().messageId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/storage-uploads")
    public ResponseEntity<Void> consumeStorageUploads(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody PubSubPushRequest request) {
        if (!pubSubOidcTokenValidator.isValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String requestId = requestId();
        try {
            String json = decodeData(request.message().data());
            StorageFinalizePayload payload = objectMapper.readValue(json, StorageFinalizePayload.class);

            if (!"OBJECT_FINALIZE".equals(payload.eventType())) {
                log.info("[{}] Ignoring non-finalize storage eventType={} messageId={}",
                        requestId,
                        payload.eventType(),
                        request.message().messageId());
                return ResponseEntity.noContent().build();
            }

            Integer clientId = extractClientIdFromObjectPath(payload.objectId());
            log.info("[{}] Received storage finalize messageId={} bucket={} objectId={} clientId={} topic={}",
                    requestId,
                    request.message().messageId(),
                    payload.bucketId(),
                    payload.objectId(),
                    clientId,
                    pubSubProperties.storageUploadsTopicName());

            notificationService.publishStorageObjectFinalized(
                    payload.bucketId(),
                    payload.objectId(),
                    clientId,
                    requestId);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("[{}] Failed to process storage-upload webhook messageId={}", requestId,
                    request.message().messageId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Integer extractClientIdFromObjectPath(@Nullable String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }

        String[] parts = objectPath.split("/");
        if (parts.length < 2 || !"clients".equals(parts[0])) {
            return null;
        }

        try {
            return Integer.valueOf(parts[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String decodeData(String data) {
        byte[] decoded = Base64.getDecoder().decode(data);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record PubSubPushRequest(
            @NotNull PushMessage message,
            @NotBlank String subscription) {
    }

    public record PushMessage(
            @NotBlank String messageId,
            @NotBlank String data,
            OffsetDateTime publishTime,
            @NotNull Map<String, String> attributes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StorageFinalizePayload(
            String bucketId,
            String objectId,
            String eventType,
            String payloadFormat,
            String objectGeneration,
            String eventTime) {
    }
}
