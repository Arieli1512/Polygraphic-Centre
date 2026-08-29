package com.drobnyd.drobnyd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "printing_points")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class PrintingPoint {

    public static PrintingPoint create(
            String name,
            String streetAddress,
            String city,
            String postalCode,
            String country,
            Integer hourlyOrderLimit) {
        PrintingPoint printingPoint = new PrintingPoint();
        printingPoint.setName(name);
        printingPoint.setStreetAddress(streetAddress);
        printingPoint.setCity(city);
        printingPoint.setPostalCode(postalCode);
        printingPoint.setCountry(country);
        printingPoint.setHourlyOrderLimit(hourlyOrderLimit);
        return printingPoint;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "printing_point_id", nullable = false, updatable = false)
    @ToString.Include
    private Integer printingPointId;

    @Column(nullable = false, length = 255)
    @ToString.Include
    private String name;

    @Column(name = "street_address", nullable = false, length = 255)
    @ToString.Include
    private String streetAddress;

    @Column(nullable = false, length = 100)
    @ToString.Include
    private String city;

    @Column(name = "postal_code", nullable = false, length = 20)
    @ToString.Include
    private String postalCode;

    @Column(nullable = false, length = 100)
    @ToString.Include
    private String country;

    @Column(name = "hourly_order_limit", nullable = false)
    @ToString.Include
    private Integer hourlyOrderLimit;
}