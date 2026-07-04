package com.drobnyd.drobnyd.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.Client;
import com.drobnyd.drobnyd.entity.Order;
import com.drobnyd.drobnyd.entity.OrderStatus;
import com.drobnyd.drobnyd.entity.PrintSettings;
import com.drobnyd.drobnyd.entity.PrintingPoint;
import com.drobnyd.drobnyd.exception.InvalidOrderTransitionException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ClientRepository;
import com.drobnyd.drobnyd.repository.OrderRepository;
import com.drobnyd.drobnyd.repository.PrintSettingsRepository;
import com.drobnyd.drobnyd.repository.PrintingPointRepository;
import com.drobnyd.drobnyd.service.model.BalanceCheckResult;
import com.drobnyd.drobnyd.service.model.OrderCreationCommand;
import com.drobnyd.drobnyd.service.model.OrderWorkflowResult;
import com.drobnyd.drobnyd.service.model.PriceEstimate;
import com.drobnyd.drobnyd.service.model.PricingRequest;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final PrintSettingsRepository printSettingsRepository;
    private final ClientRepository clientRepository;
    private final PrintingPointRepository printingPointRepository;
    private final PricingService pricingService;
    private final BalanceService balanceService;
    private final NotificationService notificationService;

    public OrderService(
            OrderRepository orderRepository,
            PrintSettingsRepository printSettingsRepository,
            ClientRepository clientRepository,
            PrintingPointRepository printingPointRepository,
            PricingService pricingService,
            BalanceService balanceService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.printSettingsRepository = printSettingsRepository;
        this.clientRepository = clientRepository;
        this.printingPointRepository = printingPointRepository;
        this.pricingService = pricingService;
        this.balanceService = balanceService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderWorkflowResult createOrder(OrderCreationCommand command) {
        log.info("Creating order for client {} at printing point {}", command.clientId(), command.printingPointId());

        Client client = clientRepository.findById(command.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", command.clientId().toString()));
        PrintingPoint printingPoint = printingPointRepository.findById(command.printingPointId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintingPoint",
                        command.printingPointId().toString()));

        PriceEstimate priceEstimate = pricingService.calculateEstimate(new PricingRequest(
                command.printingPointId(),
                command.format(),
                command.paperType(),
                command.colorMode(),
                command.duplex(),
                command.orientation(),
                command.finishing(),
                command.copies(),
                command.pageCount()));

        BalanceCheckResult balanceCheck = balanceService.verifyBalance(command.clientId(), priceEstimate.totalPrice());
        if (!balanceCheck.sufficient()) {
            throw new com.drobnyd.drobnyd.exception.InsufficientBalanceException(
                    command.clientId(),
                    priceEstimate.totalPrice(),
                    balanceCheck.currentBalance());
        }

        Order savedOrder = orderRepository.save(Order.pending(
                printingPoint,
                client,
                command.filePath(),
                command.pageCount(),
                priceEstimate.totalPrice(),
                command.pickupAt()));

        printSettingsRepository.save(PrintSettings.forOrder(
                savedOrder,
                command.format(),
                command.paperType(),
                command.colorMode(),
                command.duplex(),
                command.orientation(),
                command.finishing(),
                command.copies()));

        var updatedWallet = balanceService.debitWallet(command.clientId(), priceEstimate.totalPrice());
        notificationService.publishWalletDebited(command.clientId(), priceEstimate.totalPrice(),
                updatedWallet.getBalance());
        notificationService.publishOrderCreated(savedOrder);

        return new OrderWorkflowResult(
                savedOrder.getOrderId(),
                client.getClientId(),
                printingPoint.getPrintingPointId(),
                savedOrder.getStatus(),
                savedOrder.getTotalPrice(),
                savedOrder.getPickupAt());
    }

    @Transactional
    public OrderWorkflowResult transitionStatus(Integer orderId, OrderStatus targetStatus) {
        log.info("Transitioning order {} to status {}", orderId, targetStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        OrderStatus previousStatus = order.getStatus();
        if (!isTransitionAllowed(previousStatus, targetStatus)) {
            throw new InvalidOrderTransitionException(orderId, previousStatus, targetStatus);
        }

        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);
        notificationService.publishOrderStatusChanged(savedOrder, previousStatus);

        return new OrderWorkflowResult(
                savedOrder.getOrderId(),
                savedOrder.getClient().getClientId(),
                savedOrder.getPrintingPoint().getPrintingPointId(),
                savedOrder.getStatus(),
                savedOrder.getTotalPrice(),
                savedOrder.getPickupAt());
    }

    @Transactional(readOnly = true)
    public List<Order> listOrdersForClient(Integer clientId) {
        log.info("Listing orders for client {}", clientId);
        return orderRepository.findByClient_ClientIdOrderByCreatedAtDesc(clientId);
    }

    private boolean isTransitionAllowed(OrderStatus currentStatus, OrderStatus targetStatus) {
        return switch (currentStatus) {
            case PENDING -> targetStatus == OrderStatus.APPROVED || targetStatus == OrderStatus.CANCELLED;
            case APPROVED -> targetStatus == OrderStatus.READY || targetStatus == OrderStatus.CANCELLED;
            case READY -> targetStatus == OrderStatus.DISPENSED;
            case DISPENSED, CANCELLED -> false;
        };
    }
}