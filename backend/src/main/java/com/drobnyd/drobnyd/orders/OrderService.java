package com.drobnyd.drobnyd.orders;

import com.drobnyd.drobnyd.orders.Order;
import com.drobnyd.drobnyd.orders.events.OrderStatusChangedEvent;
import com.drobnyd.drobnyd.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void changeOrderStatus(Integer orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zamówienia o ID: " + orderId));

        order.setStatus(newStatus);
        orderRepository.save(order);

        String clientEmail = order.getClient().getEmail();

        eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), clientEmail, newStatus));
    }
}