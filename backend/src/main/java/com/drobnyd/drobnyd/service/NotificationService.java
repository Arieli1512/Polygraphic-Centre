package com.drobnyd.drobnyd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.drobnyd.drobnyd.entity.Order;
import com.drobnyd.drobnyd.entity.OrderStatus;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void publishOrderCreated(Order order) {
        log.info("Publishing order-created event for orderId={}, clientId={}, printingPointId={}, totalPrice={}",
                order.getOrderId(),
                order.getClient().getClientId(),
                order.getPrintingPoint().getPrintingPointId(),
                order.getTotalPrice());
    }

    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus) {
        log.info("Publishing order-status-changed event for orderId={}, previousStatus={}, currentStatus={}",
                order.getOrderId(),
                previousStatus,
                order.getStatus());
    }

    public void publishWalletDebited(Integer clientId, long amount, long newBalance) {
        log.info("Publishing wallet-debited event for clientId={}, amount={}, newBalance={}",
                clientId,
                amount,
                newBalance);
    }
}