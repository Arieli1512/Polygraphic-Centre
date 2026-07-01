package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.PrintingPoint;

public interface PrintingPointRepository extends JpaRepository<PrintingPoint, Integer> {
}