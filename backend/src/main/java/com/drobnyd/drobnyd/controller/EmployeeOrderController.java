package com.drobnyd.drobnyd.controller;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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
import com.drobnyd.drobnyd.entity.Printout;
import com.drobnyd.drobnyd.entity.PrintSettings;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.service.GcsSignedUrlService;
import com.drobnyd.drobnyd.service.NotificationService;
import com.drobnyd.drobnyd.service.OrderService;
import com.drobnyd.drobnyd.service.model.OrderWorkflowResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/employee/orders")
@PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
public class EmployeeOrderController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeOrderController.class);

    private final OrderService orderService;
    private final GcsSignedUrlService gcsSignedUrlService;
    private final NotificationService notificationService;
    private final ApiResponseFactory apiResponseFactory;

    public EmployeeOrderController(
            OrderService orderService,
            GcsSignedUrlService gcsSignedUrlService,
            NotificationService notificationService,
            ApiResponseFactory apiResponseFactory) {
        this.orderService = orderService;
        this.gcsSignedUrlService = gcsSignedUrlService;
        this.notificationService = notificationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping
    public ApiSuccessResponse<List<EmployeeQueueOrderResponse>> listQueue(
            Authentication authentication,
            @RequestParam(required = false) OrderStatus status) {
        SessionUser user = requireEmployeeSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        List<OrderStatus> statuses = status == null
                ? List.of(OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.READY, OrderStatus.PROBLEM_REPORTED)
                : List.of(status);

        String requestId = requestId();
        log.info("[{}] Listing employee queue for printingPointId={}, statusFilter={}", requestId, printingPointId,
                status);

        List<EmployeeQueueOrderResponse> payload = orderService.listOrdersForPrintingPoint(printingPointId, statuses)
                .stream()
                .map(order -> {
                    boolean inProgress = orderService.findPrintoutForOrder(order.getOrderId()).isPresent();
                    return new EmployeeQueueOrderResponse(
                            order.getOrderId(),
                            order.getClient().getClientId(),
                            order.getClient().getFirstName() + " " + order.getClient().getLastName(),
                            extractFileName(order.getFilePath()),
                            order.getPageCount(),
                            order.getStatus(),
                            order.getPickupAt(),
                            order.getTotalPrice(),
                            inProgress);
                })
                .toList();

        return apiResponseFactory.success(payload);
    }

    @GetMapping("/{orderId}")
    public ApiSuccessResponse<EmployeeOrderDetailsResponse> getOrderDetails(
            Authentication authentication,
            @PathVariable Integer orderId) {
        SessionUser user = requireEmployeeSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        String requestId = requestId();
        log.info("[{}] Loading employee order details for orderId={}, printingPointId={}", requestId, orderId,
                printingPointId);

        Order order = orderService.getOrderForPrintingPoint(printingPointId, orderId);
        PrintSettings printSettings = orderService.getPrintSettingsForOrder(orderId);
        Printout printout = orderService.findPrintoutForOrder(orderId).orElse(null);

        EmployeeOrderDetailsResponse payload = new EmployeeOrderDetailsResponse(
                order.getOrderId(),
                order.getClient().getClientId(),
                order.getClient().getFirstName() + " " + order.getClient().getLastName(),
                order.getClient().getEmail(),
                order.getPrintingPoint().getPrintingPointId(),
                extractFileName(order.getFilePath()),
                order.getFilePath(),
                order.getPageCount(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getPickupAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                printout != null,
                printout == null ? null : printout.getPrintedAt(),
                new PrintSettingsResponse(
                        printSettings.getFormat(),
                        printSettings.getPaperType(),
                        printSettings.getColorMode(),
                        printSettings.getDuplex(),
                        printSettings.getOrientation(),
                        printSettings.getFinishing(),
                        printSettings.getCopies()));

        return apiResponseFactory.success(payload);
    }

    @PostMapping("/{orderId}/status")
    public ApiSuccessResponse<EmployeeOrderWorkflowResponse> updateOrderStatus(
            Authentication authentication,
            @PathVariable Integer orderId,
            @Valid @RequestBody EmployeeStatusUpdateRequest request) {
        SessionUser user = requireEmployeeSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        String requestId = requestId();
        log.info("[{}] Updating employee order status orderId={}, targetStatus={}", requestId, orderId,
                request.targetStatus());

        orderService.getOrderForPrintingPoint(printingPointId, orderId);
        OrderWorkflowResult result = orderService.transitionStatus(orderId, request.targetStatus());

        return apiResponseFactory.success(new EmployeeOrderWorkflowResponse(
                result.orderId(),
                result.clientId(),
                result.printingPointId(),
                result.status(),
                result.totalPrice(),
                result.pickupAt()));
    }

    @PostMapping("/{orderId}/in-progress")
    public ApiSuccessResponse<EmployeeInProgressResponse> markInProgress(
            Authentication authentication,
            @PathVariable Integer orderId) {
        SessionUser user = requireEmployeeSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        String requestId = requestId();
        log.info("[{}] Marking order {} as in-progress", requestId, orderId);

        Printout printout = orderService.markOrderInProgress(printingPointId, user.localId(), orderId);

        return apiResponseFactory.success(new EmployeeInProgressResponse(
                printout.getOrderId(),
                printout.getOperator().getOperatorId(),
                printout.getPrintedAt()));
    }

    @PostMapping("/{orderId}/issue-report")
    public ApiSuccessResponse<EmployeeIssueReportResponse> reportIssue(
            Authentication authentication,
            @PathVariable Integer orderId,
            @Valid @RequestBody EmployeeIssueReportRequest request) {
        SessionUser user = requireEmployeeSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        String requestId = requestId();
        log.warn("[{}] Employee issue reported for orderId={}, reason={}", requestId, orderId, request.reason());

        OrderWorkflowResult result = orderService.reportIssue(printingPointId, orderId, request.reason());
        return apiResponseFactory.success(new EmployeeIssueReportResponse(
                result.orderId(),
                result.status(),
                request.reason(),
                "Issue reported and order processing paused."));
    }

    @PostMapping("/{orderId}/download-link")
    public ApiSuccessResponse<EmployeeDownloadLinkResponse> generateDownloadLink(
            Authentication authentication,
            @PathVariable Integer orderId) {
        SessionUser user = requireEmployeeSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        String requestId = requestId();
        log.info("[{}] Generating secure download link for orderId={}", requestId, orderId);

        Order order = orderService.getOrderForPrintingPoint(printingPointId, orderId);
        if (order.getFilePath() == null || order.getFilePath().isBlank()) {
            throw new ResourceNotFoundException("OrderFile", orderId.toString());
        }

        String auditId = UUID.randomUUID().toString();
        GcsSignedUrlService.SignedDownloadUrl signedDownloadUrl;
        try {
            signedDownloadUrl = gcsSignedUrlService.createDownloadSignedUrl(order.getFilePath());
        } catch (IllegalStateException ex) {
            throw new ResourceNotFoundException("OrderFile", orderId.toString());
        }

        log.info("[{}] Audit file-download-link-generated auditId={}, orderId={}, operatorId={}, filePath={}",
                requestId,
                auditId,
                orderId,
                user.localId(),
                order.getFilePath());
        notificationService.publishOrderFileDownloadLinkGenerated(order, user.localId(), requestId);

        return apiResponseFactory.success(new EmployeeDownloadLinkResponse(
                orderId,
                signedDownloadUrl.url(),
                signedDownloadUrl.expiresAt(),
                auditId));
    }

    private SessionUser requireEmployeeSessionUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new AuthenticationFailedException("No authenticated session");
        }

        if (sessionUser.printingPointId() == null) {
            throw new AuthenticationFailedException("Authenticated employee has no assigned printing point");
        }

        return sessionUser;
    }

    private String extractFileName(String filePath) {
        int slashIdx = filePath.lastIndexOf('/');
        if (slashIdx < 0 || slashIdx + 1 >= filePath.length()) {
            return filePath;
        }

        return filePath.substring(slashIdx + 1);
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record EmployeeQueueOrderResponse(
            Integer orderId,
            Integer clientId,
            String clientName,
            String fileName,
            Integer pageCount,
            OrderStatus status,
            OffsetDateTime pickupAt,
            long totalPrice,
            boolean inProgress) {
    }

    public record EmployeeOrderDetailsResponse(
            Integer orderId,
            Integer clientId,
            String clientName,
            String clientEmail,
            Integer printingPointId,
            String fileName,
            String filePath,
            Integer pageCount,
            long totalPrice,
            OrderStatus status,
            OffsetDateTime pickupAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            boolean inProgress,
            OffsetDateTime printedAt,
            PrintSettingsResponse printSettings) {
    }

    public record EmployeeOrderWorkflowResponse(
            Integer orderId,
            Integer clientId,
            Integer printingPointId,
            OrderStatus status,
            long totalPrice,
            OffsetDateTime pickupAt) {
    }

    public record EmployeeInProgressResponse(
            Integer orderId,
            Integer operatorId,
            OffsetDateTime printedAt) {
    }

    public record EmployeeIssueReportRequest(
            @NotBlank String reason) {
    }

    public record EmployeeIssueReportResponse(
            Integer orderId,
            OrderStatus status,
            String reason,
            String action) {
    }

    public record EmployeeStatusUpdateRequest(
            @NotNull OrderStatus targetStatus) {
    }

    public record EmployeeDownloadLinkResponse(
            Integer orderId,
            String downloadUrl,
            Instant expiresAt,
            String auditId) {
    }

    public record PrintSettingsResponse(
            String format,
            String paperType,
            com.drobnyd.drobnyd.entity.PrintColorMode colorMode,
            com.drobnyd.drobnyd.entity.PrintDuplex duplex,
            com.drobnyd.drobnyd.entity.PrintOrientation orientation,
            com.drobnyd.drobnyd.entity.PrintFinishing finishing,
            Integer copies) {
    }
}
