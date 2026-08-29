package com.drobnyd.drobnyd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.ExtraPricing;

public interface ExtraPricingRepository extends JpaRepository<ExtraPricing, Integer> {

    Optional<ExtraPricing> findByPrintingPointId(Integer printingPointId);
}