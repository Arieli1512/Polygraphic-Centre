package com.drobnyd.drobnyd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "extra_pricing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class ExtraPricing {

    public static ExtraPricing forPrintingPoint(
            PrintingPoint printingPoint,
            Long bindingPrice,
            Long staplingPrice,
            Long coverPrice) {
        ExtraPricing extraPricing = new ExtraPricing();
        extraPricing.setPrintingPoint(printingPoint);
        extraPricing.setPrintingPointId(printingPoint.getPrintingPointId());
        extraPricing.setBindingPrice(bindingPrice);
        extraPricing.setStaplingPrice(staplingPrice);
        extraPricing.setCoverPrice(coverPrice);
        return extraPricing;
    }

    @Id
    @Column(name = "printing_point_id", nullable = false, updatable = false)
    @ToString.Include
    private Integer printingPointId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "printing_point_id")
    private PrintingPoint printingPoint;

    @Column(name = "binding_price", nullable = false)
    @ToString.Include
    private Long bindingPrice = 0L;

    @Column(name = "stapling_price", nullable = false)
    @ToString.Include
    private Long staplingPrice = 0L;

    @Column(name = "cover_price", nullable = false)
    @ToString.Include
    private Long coverPrice = 0L;
}