package com.drobnyd.drobnyd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "print_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class PrintSettings {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    @ToString.Include
    private Integer orderId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 50)
    @ToString.Include
    private String format;

    @Column(name = "paper_type", nullable = false, length = 50)
    @ToString.Include
    private String paperType;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_mode", nullable = false, length = 50)
    @ToString.Include
    private PrintColorMode colorMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private PrintDuplex duplex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private PrintOrientation orientation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private PrintFinishing finishing;

    @Column(nullable = false)
    @ToString.Include
    private Integer copies;
}