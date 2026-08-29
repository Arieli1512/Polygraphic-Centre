package com.drobnyd.drobnyd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.ExtraPricing;
import com.drobnyd.drobnyd.entity.PrintDuplex;
import com.drobnyd.drobnyd.entity.PrintFinishing;
import com.drobnyd.drobnyd.entity.RateSheet;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ExtraPricingRepository;
import com.drobnyd.drobnyd.repository.RateSheetRepository;
import com.drobnyd.drobnyd.service.model.PriceEstimate;
import com.drobnyd.drobnyd.service.model.PricingRequest;

@Service
public class PricingService {

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);
    private final RateSheetRepository rateSheetRepository;
    private final ExtraPricingRepository extraPricingRepository;

    public PricingService(RateSheetRepository rateSheetRepository, ExtraPricingRepository extraPricingRepository) {
        this.rateSheetRepository = rateSheetRepository;
        this.extraPricingRepository = extraPricingRepository;
    }

    @Transactional(readOnly = true)
    public PriceEstimate calculateEstimate(PricingRequest request) {
        log.info("Calculating estimate for printing point: {}, format: {}, paperType: {}",
                request.printingPointId(), request.format(), request.paperType());

        RateSheet rateSheet = rateSheetRepository
                .findByPrintingPoint_PrintingPointIdAndIdPaperTypeAndIdFormat(
                        request.printingPointId(),
                        request.paperType(),
                        request.format())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RateSheet",
                        request.printingPointId() + ":" + request.paperType() + ":" + request.format()));

        long unitPagePrice = rateSheet.getPagePrice();
        int billableSheets = request.duplex() == PrintDuplex.DOUBLE_SIDED
                ? (int) Math.ceil(request.pageCount() / 2.0d)
                : request.pageCount();
        long basePrice = unitPagePrice * billableSheets * request.copies();
        long extrasPrice = resolveExtrasPrice(request.printingPointId(), request.finishing());
        long totalPrice = basePrice + extrasPrice;

        return new PriceEstimate(
                request.printingPointId(),
                unitPagePrice,
                billableSheets,
                request.copies(),
                basePrice,
                extrasPrice,
                totalPrice,
                "PLN");
    }

    private long resolveExtrasPrice(Integer printingPointId, PrintFinishing finishing) {
        ExtraPricing extraPricing = extraPricingRepository.findByPrintingPointId(printingPointId).orElse(null);
        if (extraPricing == null || finishing == null) {
            return 0L;
        }

        return switch (finishing) {
            case NONE -> 0L;
            case BINDING -> extraPricing.getBindingPrice();
            case STAPLING -> extraPricing.getStaplingPrice();
            case COVER -> extraPricing.getCoverPrice();
        };
    }
}