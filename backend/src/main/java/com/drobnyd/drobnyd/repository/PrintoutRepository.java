package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Printout;

public interface PrintoutRepository extends JpaRepository<Printout, Integer> {
}