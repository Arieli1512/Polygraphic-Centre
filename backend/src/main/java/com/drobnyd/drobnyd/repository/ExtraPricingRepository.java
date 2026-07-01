package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.ExtraPricing;

public interface ExtraPricingRepository extends JpaRepository<ExtraPricing, Integer> {
}