package com.drobnyd.drobnyd.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.SessionUser;
import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.entity.Order;
import com.drobnyd.drobnyd.entity.OrderStatus;
import com.drobnyd.drobnyd.entity.PrintColorMode;
import com.drobnyd.drobnyd.entity.PrintDuplex;
import com.drobnyd.drobnyd.entity.PrintFinishing;
import com.drobnyd.drobnyd.entity.PrintOrientation;
import com.drobnyd.drobnyd.entity.PrintSettings;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.service.OrderService;
import com.drobnyd.drobnyd.service.model.OrderCreationCommand;
import com.drobnyd.drobnyd.service.model.OrderWorkflowResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/client/orders")
@PreAuthorize("hasRole('CLIENT')")
public class ClientOrderController {

    private static final Logger log = LoggerFactory.getLogger(ClientOrderController.class);

    private final OrderService orderService;
    private final ApiResponseFactory apiResponseFactory;

    public ClientOrderController(OrderService orderService, ApiResponseFactory apiResponseFactory) {
        this.orderService = orderService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping
    public ApiSuccessResponse<List<OrderSummaryResponse>> listOrders(
            Authentication authentication,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer printingPointId,
            @RequestParam(required = false, defaultValue = "createdAtDesc") String sortBy) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Listing orders for client {}", requestId, user.localId());

        Comparator<Order> comparator = switch (sortBy) {
            case "pickupAtAsc" ->
                Comparator.comparing(Order::getPickupAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "pickupAtDesc" ->
                Comparator.comparing(Order::getPickupAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed();
            case "createdAtAsc" ->
                Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default ->
                Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };

        List<OrderSummaryResponse> payload = orderService.listOrdersForClient(user.localId()).stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> printingPointId == null
                        || order.getPrintingPoint().getPrintingPointId().equals(printingPointId))
                .sorted(comparator)
                .map(this::toSummary)
                .toList();

        return apiResponseFactory.success(payload);
    }

    @GetMapping("/{orderId}")
    public ApiSuccessResponse<OrderDetailsResponse> getOrderDetails(
            Authentication authentication,
            @PathVariable Integer orderId) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Loading order {} for client {}", requestId, orderId, user.localId());

        Order order = orderService.getOrderForClient(user.localId(), orderId);
        PrintSettings printSettings = orderService.getPrintSettingsForOrder(order.getOrderId());

        OrderDetailsResponse payload = new OrderDetailsResponse(
                order.getOrderId(),
                order.getPrintingPoint().getPrintingPointId(),
                extractFileName(order.getFilePath()),
                order.getFilePath(),
                order.getPageCount(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getPickupAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                new PrintSettingsResponse(
                        printSettings.getFormat(),
                        printSettings.getPaperType(),
                        printSettings.getColorMode(),
                        printSettings.getDuplex(),
                        printSettings.getOrientation(),
                        printSettings.getFinishing(),
                        printSettings.getCopies()),
                buildTimeline(order));

        return apiResponseFactory.success(payload);
    }

    @PostMapping
    public ApiSuccessResponse<OrderWorkflowResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Creating order for client {}", requestId, user.localId());

        OrderWorkflowResult result = orderService.createOrder(new OrderCreationCommand(
                user.localId(),
                request.printingPointId(),
                request.filePath(),
                request.pageCount(),
                request.pickupAt(),
                request.format(),
                request.paperType(),
                request.colorMode(),
                request.duplex(),
                request.orientation(),
                request.finishing(),
                request.copies()));

        return apiResponseFactory.success(new OrderWorkflowResponse(
                result.orderId(),
                result.clientId(),
                result.printingPointId(),
                result.status(),
                result.totalPrice(),
                result.pickupAt()));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiSuccessResponse<OrderWorkflowResponse> cancelOrder(
            Authentication authentication,
            @PathVariable Integer orderId) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Cancelling order {} for client {}", requestId, orderId, user.localId());

        // Ensure the order belongs to the authenticated client before transition.
        orderService.getOrderForClient(user.localId(), orderId);

        OrderWorkflowResult result = orderService.transitionStatus(orderId, OrderStatus.CANCELLED);

        return apiResponseFactory.success(new OrderWorkflowResponse(
                result.orderId(),
                result.clientId(),
                result.printingPointId(),
                result.status(),
                result.totalPrice(),
                result.pickupAt()));
    }

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getOrderId(),
                order.getPrintingPoint().getPrintingPointId(),
                extractFileName(order.getFilePath()),
                order.getTotalPrice(),
                order.getStatus(),
                order.getPickupAt(),
                order.getCreatedAt());
    }

    private List<OrderStatusEventResponse> buildTimeline(Order order) {
        List<OrderStatusEventResponse> timeline = new ArrayList<>();

        timeline.add(new OrderStatusEventResponse(
                OrderStatus.PENDING,
                order.getCreatedAt(),
                "Order created"));

        if (order.getStatus() != OrderStatus.PENDING) {
            timeline.add(new OrderStatusEventResponse(
                    order.getStatus(),
                    order.getUpdatedAt(),
                    "Current order status"));
        }

        return timeline;
    }

    private String extractFileName(String filePath) {
        int slashIdx = filePath.lastIndexOf('/');
        if (slashIdx < 0 || slashIdx + 1 >= filePath.length()) {
            return filePath;
        }

        return filePath.substring(slashIdx + 1);
    }

    private SessionUser requireSessionUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new AuthenticationFailedException("No authenticated session");
        }
        return sessionUser;
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record CreateOrderRequest(
            @NotNull Integer printingPointId,
            @NotBlank String filePath,
            @NotNull @Min(1) Integer pageCount,
            @NotNull OffsetDateTime pickupAt,
            @NotBlank String format,
            @NotBlank String paperType,
            @NotNull PrintColorMode colorMode,
            @NotNull PrintDuplex duplex,
            @NotNull PrintOrientation orientation,
            @NotNull PrintFinishing finishing,
            @NotNull @Min(1) Integer copies) {
    }

    public record OrderWorkflowResponse(
            Integer orderId,
            Integer clientId,
            Integer printingPointId,
            OrderStatus status,
            long totalPrice,
            OffsetDateTime pickupAt) {
    }

    public record OrderSummaryResponse(
            Integer orderId,
            Integer printingPointId,
            String fileName,
            long totalPrice,
            OrderStatus status,
            OffsetDateTime pickupAt,
            OffsetDateTime createdAt) {
    }

    public record OrderDetailsResponse(
            Integer orderId,
            Integer printingPointId,
            String fileName,
            String filePath,
            Integer pageCount,
            long totalPrice,
            OrderStatus status,
            OffsetDateTime pickupAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            PrintSettingsResponse printSettings,
            List<OrderStatusEventResponse> timeline) {
    }

    public record PrintSettingsResponse(
            String format,
            String paperType,
            PrintColorMode colorMode,
            PrintDuplex duplex,
            PrintOrientation orientation,
            PrintFinishing finishing,
            Integer copies) {
    }

    public record OrderStatusEventResponse(
            OrderStatus status,
            OffsetDateTime changedAt,
            String note) {
    }
}
