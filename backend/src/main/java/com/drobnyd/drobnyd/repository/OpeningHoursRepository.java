package com.drobnyd.drobnyd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.OpeningHours;
import com.drobnyd.drobnyd.entity.id.OpeningHoursId;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, OpeningHoursId> {

    List<OpeningHours> findByPrintingPoint_PrintingPointIdOrderByIdDayOfWeekAsc(Integer printingPointId);

    void deleteByPrintingPoint_PrintingPointIdAndIdDayOfWeek(Integer printingPointId, Integer dayOfWeek);
}