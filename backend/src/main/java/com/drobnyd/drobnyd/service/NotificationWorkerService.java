package com.drobnyd.drobnyd.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.drobnyd.drobnyd.config.properties.NotificationMailProperties;
import com.drobnyd.drobnyd.config.properties.PubSubProperties;
import com.drobnyd.drobnyd.service.model.NotificationDispatchEvent;
import com.drobnyd.drobnyd.service.model.OrderEventMessage;
import com.drobnyd.drobnyd.service.model.OrderEventType;

@Service
public class NotificationWorkerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorkerService.class);

    private final JavaMailSender javaMailSender;
    private final NotificationMailProperties notificationMailProperties;
    private final PubSubProperties pubSubProperties;
    private final PubSubEventPublisher pubSubEventPublisher;

    public NotificationWorkerService(
            JavaMailSender javaMailSender,
            NotificationMailProperties notificationMailProperties,
            PubSubProperties pubSubProperties,
            PubSubEventPublisher pubSubEventPublisher) {
        this.javaMailSender = javaMailSender;
        this.notificationMailProperties = notificationMailProperties;
        this.pubSubProperties = pubSubProperties;
        this.pubSubEventPublisher = pubSubEventPublisher;
    }

    public void handleOrderEvent(OrderEventMessage eventMessage) {
        log.info("Handling order event for notifications eventId={} eventType={} orderId={}",
                eventMessage.eventId(),
                eventMessage.eventType(),
                eventMessage.orderId());

        if (!shouldNotify(eventMessage.eventType())) {
            publishDispatchEvent(eventMessage, "SKIPPED", eventMessage.clientEmail(), "Event type ignored for mail");
            return;
        }

        if (eventMessage.clientEmail() == null || eventMessage.clientEmail().isBlank()) {
            publishDispatchEvent(eventMessage, "SKIPPED", null, "Missing client email");
            return;
        }

        if (!notificationMailProperties.enabled()) {
            log.info("Mail notifications disabled - simulated dispatch for eventId={} recipient={}",
                    eventMessage.eventId(),
                    eventMessage.clientEmail());
            publishDispatchEvent(eventMessage, "DISABLED", eventMessage.clientEmail(), "Mail delivery disabled");
            return;
        }

        String subject = buildSubject(eventMessage);
        String body = buildBody(eventMessage);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(notificationMailProperties.fromAddress());
        mailMessage.setTo(eventMessage.clientEmail());
        mailMessage.setSubject(subject);
        mailMessage.setText(body);

        try {
            javaMailSender.send(mailMessage);
            publishDispatchEvent(eventMessage, "SENT", eventMessage.clientEmail(), subject);
        } catch (Exception ex) {
            publishDispatchEvent(eventMessage, "FAILED", eventMessage.clientEmail(), ex.getMessage());
            throw ex;
        }
    }

    private boolean shouldNotify(OrderEventType eventType) {
        return switch (eventType) {
            case ORDER_CREATED, ORDER_STATUS_CHANGED, ORDER_IN_PROGRESS, ORDER_ISSUE_REPORTED -> true;
            case ORDER_FILE_DOWNLOAD_LINK_GENERATED, FILE_UPLOADED -> false;
        };
    }

    private String buildSubject(OrderEventMessage eventMessage) {
        String prefix = notificationMailProperties.subjectPrefix();
        return switch (eventMessage.eventType()) {
            case ORDER_CREATED ->
                prefix + " Potwierdzenie przyjecia zamowienia #" + safeOrderId(eventMessage.orderId());
            case ORDER_STATUS_CHANGED -> prefix + " Zmiana statusu zamowienia #" + safeOrderId(eventMessage.orderId());
            case ORDER_IN_PROGRESS ->
                prefix + " Zamowienie #" + safeOrderId(eventMessage.orderId()) + " jest w realizacji";
            case ORDER_ISSUE_REPORTED -> prefix + " Problem z zamowieniem #" + safeOrderId(eventMessage.orderId());
            case ORDER_FILE_DOWNLOAD_LINK_GENERATED, FILE_UPLOADED -> prefix + " Informacja o zamowieniu";
        };
    }

    private String buildBody(OrderEventMessage eventMessage) {
        StringBuilder body = new StringBuilder();
        body.append("Witaj,\n\n");
        body.append("W systemie Polygraphic Centre odnotowano zdarzenie dla Twojego zamowienia.\n\n");
        body.append("Typ zdarzenia: ").append(eventMessage.eventType()).append("\n");
        body.append("Id zamowienia: ").append(safeOrderId(eventMessage.orderId())).append("\n");

        if (eventMessage.currentStatus() != null) {
            body.append("Aktualny status: ").append(eventMessage.currentStatus()).append("\n");
        }
        if (eventMessage.reason() != null) {
            body.append("Szczegoly: ").append(eventMessage.reason()).append("\n");
        }

        body.append("\nPozdrawiamy,\nPolygraphic Centre");
        return body.toString();
    }

    private String safeOrderId(Integer orderId) {
        return orderId == null ? "N/A" : String.valueOf(orderId);
    }

    private void publishDispatchEvent(
            OrderEventMessage sourceEvent,
            String status,
            String recipient,
            String detail) {
        NotificationDispatchEvent dispatchEvent = new NotificationDispatchEvent(
                UUID.randomUUID().toString(),
                sourceEvent.eventId(),
                sourceEvent.eventType(),
                OffsetDateTime.now(),
                "EMAIL",
                status,
                recipient,
                detail,
                sourceEvent.requestId());

        pubSubEventPublisher.publish(
                pubSubProperties.notificationEventsTopicName(),
                dispatchEvent,
                Map.of(
                        "eventType", "NOTIFICATION_DISPATCH",
                        "deliveryStatus", status));
    }
}
