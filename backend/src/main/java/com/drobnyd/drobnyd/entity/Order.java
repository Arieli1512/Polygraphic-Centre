package com.drobnyd.drobnyd.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class Order {

    public static Order pending(
            PrintingPoint printingPoint,
            Client client,
            String filePath,
            Integer pageCount,
            Long totalPrice,
            OffsetDateTime pickupAt) {
        Order order = new Order();
        order.setPrintingPoint(printingPoint);
        order.setClient(client);
        order.setFilePath(filePath);
        order.setPageCount(pageCount);
        order.setTotalPrice(totalPrice);
        order.setPickupAt(pickupAt);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, updatable = false)
    @ToString.Include
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "printing_point_id", nullable = false)
    private PrintingPoint printingPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "file_path", nullable = false, columnDefinition = "text")
    @ToString.Include
    private String filePath;

    @Column(name = "page_count", nullable = false)
    @ToString.Include
    private Integer pageCount;

    @Column(name = "total_price", nullable = false)
    @ToString.Include
    private Long totalPrice;

    @Column(name = "pickup_at", nullable = false)
    @ToString.Include
    private OffsetDateTime pickupAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private OrderStatus status = OrderStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}