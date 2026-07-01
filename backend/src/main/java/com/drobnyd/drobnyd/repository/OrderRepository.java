package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}