package com.drobnyd.drobnyd.service;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.ExtraPricing;
import com.drobnyd.drobnyd.entity.OpeningHours;
import com.drobnyd.drobnyd.entity.PrintingPoint;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ExtraPricingRepository;
import com.drobnyd.drobnyd.repository.OpeningHoursRepository;
import com.drobnyd.drobnyd.repository.PrintingPointRepository;
import com.drobnyd.drobnyd.service.model.ExtraPricingSnapshot;
import com.drobnyd.drobnyd.service.model.OpeningHoursWindow;
import com.drobnyd.drobnyd.service.model.PrintShopDetails;

@Service
public class PrintShopService {

    private static final Logger log = LoggerFactory.getLogger(PrintShopService.class);
    private final PrintingPointRepository printingPointRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private final ExtraPricingRepository extraPricingRepository;

    public PrintShopService(
            PrintingPointRepository printingPointRepository,
            OpeningHoursRepository openingHoursRepository,
            ExtraPricingRepository extraPricingRepository) {
        this.printingPointRepository = printingPointRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.extraPricingRepository = extraPricingRepository;
    }

    @Transactional(readOnly = true)
    public List<PrintingPoint> listPrintingPoints() {
        log.info("Listing all printing points");
        return printingPointRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PrintShopDetails getPrintingPointDetails(Integer printingPointId) {
        log.info("Loading printing point details for: {}", printingPointId);
        PrintingPoint printingPoint = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        List<OpeningHoursWindow> openingHours = openingHoursRepository
                .findByPrintingPoint_PrintingPointIdOrderByIdDayOfWeekAsc(printingPointId)
                .stream()
                .map(this::toOpeningHoursWindow)
                .toList();

        return new PrintShopDetails(
                printingPoint.getPrintingPointId(),
                printingPoint.getName(),
                printingPoint.getStreetAddress(),
                printingPoint.getCity(),
                printingPoint.getPostalCode(),
                printingPoint.getCountry(),
                printingPoint.getHourlyOrderLimit(),
                openingHours,
                toExtraPricingSnapshot(extraPricingRepository.findByPrintingPointId(printingPointId).orElse(null)));
    }

    private OpeningHoursWindow toOpeningHoursWindow(OpeningHours openingHours) {
        return new OpeningHoursWindow(
                openingHours.getId().getDayOfWeek(),
                openingHours.getStartTime(),
                openingHours.getEndTime());
    }

    private @Nullable ExtraPricingSnapshot toExtraPricingSnapshot(@Nullable ExtraPricing extraPricing) {
        if (extraPricing == null) {
            return null;
        }
        return new ExtraPricingSnapshot(
                extraPricing.getBindingPrice(),
                extraPricing.getStaplingPrice(),
                extraPricing.getCoverPrice());
    }
}