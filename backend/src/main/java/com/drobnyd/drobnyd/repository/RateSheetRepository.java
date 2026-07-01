package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.RateSheet;
import com.drobnyd.drobnyd.entity.id.RateSheetId;

public interface RateSheetRepository extends JpaRepository<RateSheet, RateSheetId> {
}