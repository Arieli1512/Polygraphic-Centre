package com.drobnyd.drobnyd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.RateSheet;
import com.drobnyd.drobnyd.entity.id.RateSheetId;

public interface RateSheetRepository extends JpaRepository<RateSheet, RateSheetId> {

    List<RateSheet> findByPrintingPoint_PrintingPointIdOrderByIdPaperTypeAscIdFormatAsc(Integer printingPointId);

    Optional<RateSheet> findByPrintingPoint_PrintingPointIdAndIdPaperTypeAndIdFormat(
            Integer printingPointId,
            String paperType,
            String format);
}