package com.drobnyd.drobnyd.orders;

import com.drobnyd.drobnyd.clients.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "pickup_at", nullable = false)
    private OffsetDateTime pickupAt;

    @Column(nullable = false)
    private String status = "PENDING";
}