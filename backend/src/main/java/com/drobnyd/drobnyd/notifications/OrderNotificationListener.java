package com.drobnyd.drobnyd.notifications;

import com.drobnyd.drobnyd.orders.events.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;

@Component
public class OrderNotificationListener {

    private final EmailService emailService;

    public OrderNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChange(OrderStatusChangedEvent event) {
        emailService.sendOrderStatusEmail(event.clientEmail(), event.newStatus());
    }
}