package com.drobnyd.drobnyd.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.Client;
import com.drobnyd.drobnyd.entity.Operator;
import com.drobnyd.drobnyd.entity.Order;
import com.drobnyd.drobnyd.entity.OrderStatus;
import com.drobnyd.drobnyd.entity.Printout;
import com.drobnyd.drobnyd.entity.PrintSettings;
import com.drobnyd.drobnyd.entity.PrintingPoint;
import com.drobnyd.drobnyd.exception.InvalidOrderTransitionException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ClientRepository;
import com.drobnyd.drobnyd.repository.OperatorRepository;
import com.drobnyd.drobnyd.repository.OrderRepository;
import com.drobnyd.drobnyd.repository.PrintoutRepository;
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
        private final PrintoutRepository printoutRepository;
        private final ClientRepository clientRepository;
        private final OperatorRepository operatorRepository;
        private final PrintingPointRepository printingPointRepository;
        private final PricingService pricingService;
        private final BalanceService balanceService;
        private final NotificationService notificationService;

        public OrderService(
                        OrderRepository orderRepository,
                        PrintSettingsRepository printSettingsRepository,
                        PrintoutRepository printoutRepository,
                        ClientRepository clientRepository,
                        OperatorRepository operatorRepository,
                        PrintingPointRepository printingPointRepository,
                        PricingService pricingService,
                        BalanceService balanceService,
                        NotificationService notificationService) {
                this.orderRepository = orderRepository;
                this.printSettingsRepository = printSettingsRepository;
                this.printoutRepository = printoutRepository;
                this.clientRepository = clientRepository;
                this.operatorRepository = operatorRepository;
                this.printingPointRepository = printingPointRepository;
                this.pricingService = pricingService;
                this.balanceService = balanceService;
                this.notificationService = notificationService;
        }

        @Transactional
        public OrderWorkflowResult createOrder(OrderCreationCommand command) {
                log.info("Creating order for client {} at printing point {}", command.clientId(),
                                command.printingPointId());

                Client client = clientRepository.findById(command.clientId())
                                .orElseThrow(() -> new ResourceNotFoundException("Client",
                                                command.clientId().toString()));
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

                BalanceCheckResult balanceCheck = balanceService.verifyBalance(command.clientId(),
                                priceEstimate.totalPrice());
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

                if (targetStatus == OrderStatus.CANCELLED) {
                        balanceService.recordTopUp(
                                        savedOrder.getClient().getClientId(),
                                        savedOrder.getTotalPrice());
                        var wallet = balanceService.getWallet(savedOrder.getClient().getClientId());
                        notificationService.publishWalletCredited(
                                        savedOrder.getClient().getClientId(),
                                        savedOrder.getTotalPrice(),
                                        wallet.getBalance());
                }

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

        @Transactional(readOnly = true)
        public Order getOrderForClient(Integer clientId, Integer orderId) {
                log.info("Loading order {} for client {}", orderId, clientId);
                return orderRepository.findByOrderIdAndClient_ClientId(orderId, clientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        }

        @Transactional(readOnly = true)
        public PrintSettings getPrintSettingsForOrder(Integer orderId) {
                log.info("Loading print settings for order {}", orderId);
                return printSettingsRepository.findByOrder_OrderId(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("PrintSettings", orderId.toString()));
        }

        @Transactional(readOnly = true)
        public List<Order> listOrdersForPrintingPoint(Integer printingPointId, List<OrderStatus> statuses) {
                log.info("Listing orders for printing point {} with statuses {}", printingPointId, statuses);
                return orderRepository.findByPrintingPoint_PrintingPointIdAndStatusInOrderByPickupAtAsc(
                                printingPointId,
                                statuses);
        }

        @Transactional(readOnly = true)
        public Order getOrderForPrintingPoint(Integer printingPointId, Integer orderId) {
                log.info("Loading order {} for printing point {}", orderId, printingPointId);
                return orderRepository.findByOrderIdAndPrintingPoint_PrintingPointId(orderId, printingPointId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        }

        @Transactional(readOnly = true)
        public Printout getPrintoutForOrder(Integer orderId) {
                return printoutRepository.findByOrder_OrderId(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Printout", orderId.toString()));
        }

        @Transactional(readOnly = true)
        public Optional<Printout> findPrintoutForOrder(Integer orderId) {
                return printoutRepository.findByOrder_OrderId(orderId);
        }

        @Transactional
        public Printout markOrderInProgress(Integer printingPointId, Integer operatorId, Integer orderId) {
                log.info("Marking order {} as in-progress by operator {} at printing point {}", orderId, operatorId,
                                printingPointId);

                Order order = getOrderForPrintingPoint(printingPointId, orderId);
                if (order.getStatus() != OrderStatus.APPROVED) {
                        throw new InvalidOrderTransitionException(orderId, order.getStatus(), OrderStatus.APPROVED);
                }

                Operator operator = operatorRepository
                                .findByOperatorIdAndPrintingPoint_PrintingPointId(operatorId, printingPointId)
                                .orElseThrow(() -> new ResourceNotFoundException("Operator", operatorId.toString()));

                Printout printout = printoutRepository.findByOrder_OrderId(orderId).orElseGet(() -> {
                        return Printout.started(order, operator, java.time.OffsetDateTime.now());
                });

                printout.setOperator(operator);
                printout.setPrintedAt(java.time.OffsetDateTime.now());
                Printout saved = printoutRepository.save(printout);

                notificationService.publishOrderInProgress(order, operatorId);
                return saved;
        }

        @Transactional
        public OrderWorkflowResult reportIssue(Integer printingPointId, Integer orderId, String reason) {
                log.info("Reporting issue for order {} at printing point {}", orderId, printingPointId);
                Order order = getOrderForPrintingPoint(printingPointId, orderId);

                if (!isTransitionAllowed(order.getStatus(), OrderStatus.PROBLEM_REPORTED)) {
                        throw new InvalidOrderTransitionException(orderId, order.getStatus(),
                                        OrderStatus.PROBLEM_REPORTED);
                }

                OrderStatus previousStatus = order.getStatus();
                order.setStatus(OrderStatus.PROBLEM_REPORTED);
                Order saved = orderRepository.save(order);
                notificationService.publishOrderIssueReported(saved, previousStatus, reason);

                return new OrderWorkflowResult(
                                saved.getOrderId(),
                                saved.getClient().getClientId(),
                                saved.getPrintingPoint().getPrintingPointId(),
                                saved.getStatus(),
                                saved.getTotalPrice(),
                                saved.getPickupAt());
        }

        private boolean isTransitionAllowed(OrderStatus currentStatus, OrderStatus targetStatus) {
                return switch (currentStatus) {
                        case PENDING -> targetStatus == OrderStatus.APPROVED
                                        || targetStatus == OrderStatus.CANCELLED
                                        || targetStatus == OrderStatus.PROBLEM_REPORTED;
                        case APPROVED -> targetStatus == OrderStatus.READY
                                        || targetStatus == OrderStatus.CANCELLED
                                        || targetStatus == OrderStatus.PROBLEM_REPORTED;
                        case READY -> targetStatus == OrderStatus.DISPENSED
                                        || targetStatus == OrderStatus.PROBLEM_REPORTED;
                        case PROBLEM_REPORTED -> targetStatus == OrderStatus.APPROVED
                                        || targetStatus == OrderStatus.CANCELLED;
                        case DISPENSED, CANCELLED -> false;
                };
        }
}