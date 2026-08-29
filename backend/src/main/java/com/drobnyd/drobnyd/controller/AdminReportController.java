package com.drobnyd.drobnyd.controller;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.SessionUser;
import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.service.ReportService;
import com.drobnyd.drobnyd.service.model.OrderReportSummary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ReportService reportService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminReportController(ReportService reportService, ApiResponseFactory apiResponseFactory) {
        this.reportService = reportService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/orders-summary")
    public ApiSuccessResponse<OrderReportSummaryResponse> generateSummary(
            Authentication authentication,
            @Valid @RequestBody GenerateOrderReportRequest request) {
        requireAdminSessionUser(authentication);

        OrderReportSummary summary = reportService.generateOrderSummary(
                request.from(),
                request.to(),
                request.reportType(),
                request.printingPointId());

        return apiResponseFactory.success(OrderReportSummaryResponse.from(summary));
    }

    @GetMapping("/orders-summary/export")
    public ResponseEntity<byte[]> exportSummary(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam String reportType,
            @RequestParam String format,
            @RequestParam(required = false) Integer printingPointId) {
        requireAdminSessionUser(authentication);

        OrderReportSummary summary = reportService.generateOrderSummary(from, to, reportType, printingPointId);

        String normalizedFormat = format.toUpperCase();
        String fileStamp = FILE_TS.format(OffsetDateTime.now());

        if ("CSV".equals(normalizedFormat)) {
            byte[] content = reportService.toCsv(summary);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("orders-report-" + fileStamp + ".csv")
                                    .build()
                                    .toString())
                    .body(content);
        }

        byte[] content = reportService.toPdf(summary);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("orders-report-" + fileStamp + ".pdf")
                                .build()
                                .toString())
                .body(content);
    }

    private SessionUser requireAdminSessionUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new AuthenticationFailedException("No authenticated session");
        }

        if (!"ADMIN".equalsIgnoreCase(sessionUser.role())) {
            throw new AuthenticationFailedException("Authenticated user has no ADMIN role");
        }

        return sessionUser;
    }

    public record GenerateOrderReportRequest(
            @NotNull OffsetDateTime from,
            @NotNull OffsetDateTime to,
            @NotBlank String reportType,
            Integer printingPointId) {
    }

    public record OrderReportSummaryResponse(
            OffsetDateTime from,
            OffsetDateTime to,
            String reportType,
            int totalOrders,
            long totalRevenue,
            long averageLeadTimeMinutes,
            java.util.List<OrderReportBucketResponse> buckets) {
        static OrderReportSummaryResponse from(OrderReportSummary summary) {
            return new OrderReportSummaryResponse(
                    summary.from(),
                    summary.to(),
                    summary.reportType(),
                    summary.totalOrders(),
                    summary.totalRevenue(),
                    summary.averageLeadTimeMinutes(),
                    summary.buckets().stream().map(bucket -> new OrderReportBucketResponse(
                            bucket.period(),
                            bucket.orderCount(),
                            bucket.revenue())).toList());
        }
    }

    public record OrderReportBucketResponse(
            String period,
            int orderCount,
            long revenue) {
    }
}
