package com.drobnyd.drobnyd.controller;

import java.time.LocalTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.repository.RateSheetRepository;
import com.drobnyd.drobnyd.service.PrintShopService;
import com.drobnyd.drobnyd.service.model.PrintShopDetails;

@RestController
@RequestMapping("/api/v1/printing-points")
public class PrintingPointController {

    private static final Logger log = LoggerFactory.getLogger(PrintingPointController.class);

    private final PrintShopService printShopService;
    private final RateSheetRepository rateSheetRepository;
    private final ApiResponseFactory apiResponseFactory;

    public PrintingPointController(
            PrintShopService printShopService,
            RateSheetRepository rateSheetRepository,
            ApiResponseFactory apiResponseFactory) {
        this.printShopService = printShopService;
        this.rateSheetRepository = rateSheetRepository;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping
    public ApiSuccessResponse<List<PrintingPointSummaryResponse>> listPrintingPoints() {
        String requestId = requestId();
        log.info("[{}] Listing printing points", requestId);

        List<PrintingPointSummaryResponse> payload = printShopService.listPrintingPoints().stream()
                .map(point -> new PrintingPointSummaryResponse(
                        point.getPrintingPointId(),
                        point.getName(),
                        point.getStreetAddress(),
                        point.getCity(),
                        point.getPostalCode(),
                        point.getCountry(),
                        point.getHourlyOrderLimit()))
                .toList();

        return apiResponseFactory.success(payload);
    }

    @GetMapping("/{printingPointId}")
    public ApiSuccessResponse<PrintingPointDetailsResponse> getPrintingPointDetails(
            @PathVariable Integer printingPointId) {
        String requestId = requestId();
        log.info("[{}] Loading details for printing point {}", requestId, printingPointId);

        PrintShopDetails details = printShopService.getPrintingPointDetails(printingPointId);
        List<RateOptionResponse> rateOptions = rateSheetRepository
                .findAll()
                .stream()
                .filter(rateSheet -> rateSheet.getPrintingPoint().getPrintingPointId().equals(printingPointId))
                .map(rateSheet -> new RateOptionResponse(
                        rateSheet.getId().getPaperType(),
                        rateSheet.getId().getFormat(),
                        rateSheet.getPagePrice()))
                .toList();

        PrintingPointDetailsResponse payload = new PrintingPointDetailsResponse(
                details.printingPointId(),
                details.name(),
                details.streetAddress(),
                details.city(),
                details.postalCode(),
                details.country(),
                details.hourlyOrderLimit(),
                details.openingHours().stream()
                        .map(window -> new OpeningHoursResponse(
                                window.dayOfWeek(),
                                window.startTime(),
                                window.endTime()))
                        .toList(),
                toExtraPricing(details.extraPricing()),
                rateOptions);

        return apiResponseFactory.success(payload);
    }

    private @Nullable ExtraPricingResponse toExtraPricing(
            com.drobnyd.drobnyd.service.model.@Nullable ExtraPricingSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        return new ExtraPricingResponse(
                snapshot.bindingPrice(),
                snapshot.staplingPrice(),
                snapshot.coverPrice());
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record PrintingPointSummaryResponse(
            Integer printingPointId,
            String name,
            String streetAddress,
            String city,
            String postalCode,
            String country,
            Integer hourlyOrderLimit) {
    }

    public record PrintingPointDetailsResponse(
            Integer printingPointId,
            String name,
            String streetAddress,
            String city,
            String postalCode,
            String country,
            Integer hourlyOrderLimit,
            List<OpeningHoursResponse> openingHours,
            @Nullable ExtraPricingResponse extraPricing,
            List<RateOptionResponse> rateOptions) {
    }

    public record OpeningHoursResponse(
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime) {
    }

    public record ExtraPricingResponse(
            long bindingPrice,
            long staplingPrice,
            long coverPrice) {
    }

    public record RateOptionResponse(
            String paperType,
            String format,
            long pagePrice) {
    }
}
