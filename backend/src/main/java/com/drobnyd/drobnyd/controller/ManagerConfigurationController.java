package com.drobnyd.drobnyd.controller;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.SessionUser;
import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.entity.ExtraPricing;
import com.drobnyd.drobnyd.entity.OpeningHours;
import com.drobnyd.drobnyd.entity.PrintingPoint;
import com.drobnyd.drobnyd.entity.RateSheet;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.exception.ConfigurationConflictException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ExtraPricingRepository;
import com.drobnyd.drobnyd.repository.OpeningHoursRepository;
import com.drobnyd.drobnyd.repository.PrintingPointRepository;
import com.drobnyd.drobnyd.repository.RateSheetRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@RestController
@RequestMapping("/api/v1/manager/printing-point")
@PreAuthorize("hasRole('ADMIN')")
public class ManagerConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(ManagerConfigurationController.class);

    private final PrintingPointRepository printingPointRepository;
    private final RateSheetRepository rateSheetRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private final ExtraPricingRepository extraPricingRepository;
    private final ApiResponseFactory apiResponseFactory;

    public ManagerConfigurationController(
            PrintingPointRepository printingPointRepository,
            RateSheetRepository rateSheetRepository,
            OpeningHoursRepository openingHoursRepository,
            ExtraPricingRepository extraPricingRepository,
            ApiResponseFactory apiResponseFactory) {
        this.printingPointRepository = printingPointRepository;
        this.rateSheetRepository = rateSheetRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.extraPricingRepository = extraPricingRepository;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/configuration")
    public ApiSuccessResponse<ManagerConfigurationResponse> getConfiguration(Authentication authentication) {
        SessionUser user = requireManagerSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        PrintingPoint printingPoint = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        List<RateRuleResponse> rateRules = rateSheetRepository
                .findByPrintingPoint_PrintingPointIdOrderByIdPaperTypeAscIdFormatAsc(printingPointId)
                .stream()
                .map(rate -> new RateRuleResponse(rate.getId().getPaperType(), rate.getId().getFormat(),
                        rate.getPagePrice()))
                .toList();

        List<OpeningSlotResponse> openingSlots = openingHoursRepository
                .findByPrintingPoint_PrintingPointIdOrderByIdDayOfWeekAsc(printingPointId)
                .stream()
                .map(slot -> new OpeningSlotResponse(slot.getId().getDayOfWeek(), true, slot.getStartTime(),
                        slot.getEndTime()))
                .toList();

        ExtraPricing extraPricing = extraPricingRepository.findByPrintingPointId(printingPointId).orElse(null);
        ExtraPricingResponse extraPricingResponse = extraPricing == null
                ? new ExtraPricingResponse(0L, 0L, 0L)
                : new ExtraPricingResponse(
                        extraPricing.getBindingPrice(),
                        extraPricing.getStaplingPrice(),
                        extraPricing.getCoverPrice());

        return apiResponseFactory.success(new ManagerConfigurationResponse(
                printingPoint.getPrintingPointId(),
                printingPoint.getName(),
                printingPoint.getStreetAddress(),
                printingPoint.getCity(),
                printingPoint.getPostalCode(),
                printingPoint.getCountry(),
                printingPoint.getHourlyOrderLimit(),
                rateRules,
                openingSlots,
                extraPricingResponse));
    }

    @PutMapping("/capacity")
    public ApiSuccessResponse<CapacityResponse> updateCapacity(
            Authentication authentication,
            @Valid @RequestBody UpdateCapacityRequest request) {
        SessionUser user = requireManagerSessionUser(authentication);
        PrintingPoint printingPoint = printingPointRepository.findById(user.printingPointId())
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", user.printingPointId().toString()));

        printingPoint.setHourlyOrderLimit(request.hourlyOrderLimit());
        PrintingPoint saved = printingPointRepository.save(printingPoint);

        log.info("[{}] Updated hourly capacity limit for printingPointId={} to {}",
                requestId(),
                saved.getPrintingPointId(),
                saved.getHourlyOrderLimit());

        return apiResponseFactory
                .success(new CapacityResponse(saved.getPrintingPointId(), saved.getHourlyOrderLimit()));
    }

    @PostMapping("/rate-rules")
    public ApiSuccessResponse<RateRuleResponse> createRateRule(
            Authentication authentication,
            @Valid @RequestBody UpsertRateRuleRequest request) {
        SessionUser user = requireManagerSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        rateSheetRepository
                .findByPrintingPoint_PrintingPointIdAndIdPaperTypeAndIdFormat(
                        printingPointId,
                        request.paperType(),
                        request.format())
                .ifPresent(rate -> {
                    throw new ConfigurationConflictException(
                            "Rate rule already exists for paperType=" + request.paperType() + " and format="
                                    + request.format(),
                            "Regula cennika dla wybranego papieru i formatu juz istnieje.",
                            "Zmien regule przez edycje albo usun poprzednia i sprobuj ponownie.");
                });

        PrintingPoint printingPoint = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        RateSheet saved = rateSheetRepository.save(
                RateSheet.of(printingPoint, request.paperType(), request.format(), request.pagePrice()));

        return apiResponseFactory.success(new RateRuleResponse(
                saved.getId().getPaperType(),
                saved.getId().getFormat(),
                saved.getPagePrice()));
    }

    @PutMapping("/rate-rules")
    public ApiSuccessResponse<RateRuleResponse> updateRateRule(
            Authentication authentication,
            @Valid @RequestBody UpsertRateRuleRequest request) {
        SessionUser user = requireManagerSessionUser(authentication);

        RateSheet rateSheet = rateSheetRepository
                .findByPrintingPoint_PrintingPointIdAndIdPaperTypeAndIdFormat(
                        user.printingPointId(),
                        request.paperType(),
                        request.format())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RateRule",
                        request.paperType() + ":" + request.format()));

        rateSheet.setPagePrice(request.pagePrice());
        RateSheet saved = rateSheetRepository.save(rateSheet);

        return apiResponseFactory.success(new RateRuleResponse(
                saved.getId().getPaperType(),
                saved.getId().getFormat(),
                saved.getPagePrice()));
    }

    @DeleteMapping("/rate-rules")
    public ApiSuccessResponse<DeleteRateRuleResponse> deleteRateRule(
            Authentication authentication,
            @RequestParam String paperType,
            @RequestParam String format) {
        SessionUser user = requireManagerSessionUser(authentication);

        RateSheet rateSheet = rateSheetRepository
                .findByPrintingPoint_PrintingPointIdAndIdPaperTypeAndIdFormat(
                        user.printingPointId(),
                        paperType,
                        format)
                .orElseThrow(() -> new ResourceNotFoundException("RateRule", paperType + ":" + format));

        rateSheetRepository.delete(rateSheet);
        return apiResponseFactory.success(new DeleteRateRuleResponse(paperType, format, true));
    }

    @PutMapping("/extra-pricing")
    public ApiSuccessResponse<ExtraPricingResponse> updateExtraPricing(
            Authentication authentication,
            @Valid @RequestBody UpdateExtraPricingRequest request) {
        SessionUser user = requireManagerSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        PrintingPoint printingPoint = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        ExtraPricing extraPricing = extraPricingRepository.findByPrintingPointId(printingPointId)
                .orElseGet(() -> ExtraPricing.forPrintingPoint(printingPoint, 0L, 0L, 0L));

        extraPricing.setBindingPrice(request.bindingPrice());
        extraPricing.setStaplingPrice(request.staplingPrice());
        extraPricing.setCoverPrice(request.coverPrice());

        ExtraPricing saved = extraPricingRepository.save(extraPricing);

        return apiResponseFactory.success(new ExtraPricingResponse(
                saved.getBindingPrice(),
                saved.getStaplingPrice(),
                saved.getCoverPrice()));
    }

    @PutMapping("/opening-hours")
    public ApiSuccessResponse<List<OpeningSlotResponse>> updateOpeningHours(
            Authentication authentication,
            @Valid @RequestBody UpdateOpeningHoursRequest request) {
        SessionUser user = requireManagerSessionUser(authentication);
        Integer printingPointId = user.printingPointId();

        validateSlots(request.slots());

        PrintingPoint printingPoint = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        for (OpeningSlotUpdateRequest slot : request.slots()) {
            openingHoursRepository.deleteByPrintingPoint_PrintingPointIdAndIdDayOfWeek(printingPointId,
                    slot.dayOfWeek());
            if (!slot.enabled()) {
                continue;
            }

            openingHoursRepository
                    .save(OpeningHours.of(printingPoint, slot.dayOfWeek(), slot.startTime(), slot.endTime()));
        }

        List<OpeningSlotResponse> payload = openingHoursRepository
                .findByPrintingPoint_PrintingPointIdOrderByIdDayOfWeekAsc(printingPointId)
                .stream()
                .map(slot -> new OpeningSlotResponse(slot.getId().getDayOfWeek(), true, slot.getStartTime(),
                        slot.getEndTime()))
                .toList();

        return apiResponseFactory.success(payload);
    }

    private void validateSlots(List<OpeningSlotUpdateRequest> slots) {
        Set<Integer> days = new HashSet<>();
        for (OpeningSlotUpdateRequest slot : slots) {
            if (!days.add(slot.dayOfWeek())) {
                throw new ConfigurationConflictException(
                        "Duplicate dayOfWeek in opening-hours payload: " + slot.dayOfWeek(),
                        "Nie mozna zapisac kilku slotow dla tego samego dnia w modelu v0.",
                        "Zostaw tylko jeden slot czasowy dla kazdego dnia tygodnia.");
            }

            if (!slot.enabled()) {
                continue;
            }

            if (slot.startTime() == null || slot.endTime() == null) {
                throw new ConfigurationConflictException(
                        "Missing startTime/endTime for enabled dayOfWeek=" + slot.dayOfWeek(),
                        "Slot oznaczony jako aktywny musi miec godzine startu i konca.",
                        "Uzupelnij oba pola czasu lub wylacz slot.");
            }

            if (!slot.startTime().isBefore(slot.endTime())) {
                throw new ConfigurationConflictException(
                        "Invalid slot range for dayOfWeek=" + slot.dayOfWeek(),
                        "Godzina otwarcia musi byc wczesniejsza niz godzina zamkniecia.",
                        "Popraw zakres godzin i zapisz ponownie.");
            }
        }
    }

    private SessionUser requireManagerSessionUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new AuthenticationFailedException("No authenticated session");
        }

        if (sessionUser.printingPointId() == null) {
            throw new AuthenticationFailedException("Authenticated manager has no assigned printing point");
        }

        return sessionUser;
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record ManagerConfigurationResponse(
            Integer printingPointId,
            String name,
            String streetAddress,
            String city,
            String postalCode,
            String country,
            Integer hourlyOrderLimit,
            List<RateRuleResponse> rateRules,
            List<OpeningSlotResponse> openingHours,
            ExtraPricingResponse extraPricing) {
    }

    public record RateRuleResponse(
            String paperType,
            String format,
            long pagePrice) {
    }

    public record OpeningSlotResponse(
            Integer dayOfWeek,
            boolean enabled,
            LocalTime startTime,
            LocalTime endTime) {
    }

    public record ExtraPricingResponse(
            long bindingPrice,
            long staplingPrice,
            long coverPrice) {
    }

    public record UpdateCapacityRequest(
            @Min(1) Integer hourlyOrderLimit) {
    }

    public record CapacityResponse(
            Integer printingPointId,
            Integer hourlyOrderLimit) {
    }

    public record UpsertRateRuleRequest(
            @NotBlank String paperType,
            @NotBlank String format,
            @Min(1) long pagePrice) {
    }

    public record DeleteRateRuleResponse(
            String paperType,
            String format,
            boolean deleted) {
    }

    public record UpdateExtraPricingRequest(
            @Min(0) long bindingPrice,
            @Min(0) long staplingPrice,
            @Min(0) long coverPrice) {
    }

    public record OpeningSlotUpdateRequest(
            @Min(1) @Max(7) Integer dayOfWeek,
            boolean enabled,
            LocalTime startTime,
            LocalTime endTime) {
    }

    public record UpdateOpeningHoursRequest(
            @NotEmpty List<@Valid OpeningSlotUpdateRequest> slots) {
    }

}
