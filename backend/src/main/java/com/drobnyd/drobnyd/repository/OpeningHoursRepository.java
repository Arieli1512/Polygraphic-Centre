package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.OpeningHours;
import com.drobnyd.drobnyd.entity.id.OpeningHoursId;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, OpeningHoursId> {
}