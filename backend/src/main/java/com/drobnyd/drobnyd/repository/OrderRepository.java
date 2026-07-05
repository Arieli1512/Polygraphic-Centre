package com.drobnyd.drobnyd.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.drobnyd.drobnyd.entity.Order;
import com.drobnyd.drobnyd.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @EntityGraph(attributePaths = { "printingPoint" })
    List<Order> findByClient_ClientIdOrderByCreatedAtDesc(Integer clientId);

    @EntityGraph(attributePaths = { "client", "printingPoint" })
    Optional<Order> findByOrderIdAndClient_ClientId(Integer orderId, Integer clientId);

    @EntityGraph(attributePaths = { "client", "printingPoint" })
    List<Order> findByPrintingPoint_PrintingPointIdAndStatusInOrderByPickupAtAsc(
            Integer printingPointId,
            List<OrderStatus> statuses);

    @EntityGraph(attributePaths = { "client", "printingPoint" })
    Optional<Order> findByOrderIdAndPrintingPoint_PrintingPointId(Integer orderId, Integer printingPointId);

    List<Order> findByCreatedAtBetweenOrderByCreatedAtAsc(OffsetDateTime createdFrom, OffsetDateTime createdTo);

    List<Order> findByPrintingPoint_PrintingPointIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Integer printingPointId,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo);
}