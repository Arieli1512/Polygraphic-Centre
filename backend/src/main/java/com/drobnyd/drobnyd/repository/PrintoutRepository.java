package com.drobnyd.drobnyd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Printout;

public interface PrintoutRepository extends JpaRepository<Printout, Integer> {

    Optional<Printout> findByOrder_OrderId(Integer orderId);
}