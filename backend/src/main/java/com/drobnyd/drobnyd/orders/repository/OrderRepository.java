package com.drobnyd.drobnyd.orders.repository;

import com.drobnyd.drobnyd.orders.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findAllByClientId(Integer clientId);
}