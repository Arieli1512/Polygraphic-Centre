package com.drobnyd.drobnyd.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "printouts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class Printout {

    public static Printout started(Order order, Operator operator, OffsetDateTime printedAt) {
        Printout printout = new Printout();
        printout.setOrder(order);
        printout.setOperator(operator);
        printout.setPrintedAt(printedAt);
        return printout;
    }

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    @ToString.Include
    private Integer orderId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @Column(name = "printed_at", nullable = false)
    @ToString.Include
    private OffsetDateTime printedAt;
}