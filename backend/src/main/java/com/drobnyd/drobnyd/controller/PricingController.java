package com.drobnyd.drobnyd.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.entity.PrintColorMode;
import com.drobnyd.drobnyd.entity.PrintDuplex;
import com.drobnyd.drobnyd.entity.PrintFinishing;
import com.drobnyd.drobnyd.entity.PrintOrientation;
import com.drobnyd.drobnyd.service.PricingService;
import com.drobnyd.drobnyd.service.model.PriceEstimate;
import com.drobnyd.drobnyd.service.model.PricingRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    private static final Logger log = LoggerFactory.getLogger(PricingController.class);

    private final PricingService pricingService;
    private final ApiResponseFactory apiResponseFactory;

    public PricingController(PricingService pricingService, ApiResponseFactory apiResponseFactory) {
        this.pricingService = pricingService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/estimates")
    public ApiSuccessResponse<PriceEstimateResponse> calculateEstimate(
            @Valid @RequestBody PriceEstimateRequest request) {
        String requestId = requestId();
        log.info("[{}] Calculating estimate for printing point {}", requestId, request.printingPointId());

        PriceEstimate estimate = pricingService.calculateEstimate(new PricingRequest(
                request.printingPointId(),
                request.format(),
                request.paperType(),
                request.colorMode(),
                request.duplex(),
                request.orientation(),
                request.finishing(),
                request.copies(),
                request.pageCount()));

        return apiResponseFactory.success(new PriceEstimateResponse(
                estimate.printingPointId(),
                estimate.unitPagePrice(),
                estimate.billableSheets(),
                estimate.copies(),
                estimate.basePrice(),
                estimate.extrasPrice(),
                estimate.totalPrice(),
                estimate.currency()));
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record PriceEstimateRequest(
            @NotNull Integer printingPointId,
            @NotBlank String format,
            @NotBlank String paperType,
            @NotNull PrintColorMode colorMode,
            @NotNull PrintDuplex duplex,
            @NotNull PrintOrientation orientation,
            @NotNull PrintFinishing finishing,
            @NotNull @Min(1) Integer copies,
            @NotNull @Min(1) Integer pageCount) {
    }

    public record PriceEstimateResponse(
            Integer printingPointId,
            long unitPagePrice,
            int billableSheets,
            int copies,
            long basePrice,
            long extrasPrice,
            long totalPrice,
            String currency) {
    }
}
